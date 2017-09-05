/*Java implementation of an FTP-Client
 *George Karaolanis
 *Pantelis Dimitroulis
 *University of Thessaly, second assignment
 *in object-oriented programming course*/

import java.util.*;
import java.io.*;
import java.net.*;

public class FtpClient {
  Socket controlSocket;                             /*Socket for transfering commands*/
  BufferedReader reader;                            /*Reading from input*/
  PrintWriter out;                                  /*To send through control socket*/
  BufferedReader in;                                /*To read from control socket*/
  File workingDir;                                  /*Current local directory*/

  static boolean DEBUG = false;


  enum DBG {IN, OUT};

  void dbg(DBG direction, String msg) {
    if(DEBUG) {
      if(direction == DBG.IN)
        System.err.println("<- "+msg);
      else if(direction == DBG.OUT)
        System.err.println("-> "+msg);
      else
        System.err.println(msg);
    }
  }

  public FtpClient(boolean pasv, boolean overwrite) {
    reader = new BufferedReader( new InputStreamReader(System.in) );
    workingDir = new File(".");
  }

/*The User Interface for the bind method*/

  public void bindUI(String [] args) {
    String inetAddress;
    int port=0;

    /*Check for arguments*/
    try {

      if( args!=null && args.length > 0 ) {
        inetAddress = args[0];
      }
      else {
        System.out.print("Hostname: ");
        inetAddress = reader.readLine();
      }

      if( args!=null && args.length > 1 ) {
        port = new Integer( args[1] ).intValue();
      }
      else {
        System.out.print("Port: ");
        port = new Integer( reader.readLine() ).intValue();
      }
      if( bind(inetAddress, port) ) {
        System.out.println("Socket bind OK!");
      }
      else
        System.out.println("Socket bind FAILED!");
    } catch( IOException ex) {
      ex.printStackTrace();
      System.exit(-1);
    }
  }


  /*Connect to a server on a specific port number, the actual bind method*/
  public boolean bind(String inetAddress, int port) {

      String inputLine,answer;       /*To store string read from socket*/

      try{
          controlSocket = new Socket(inetAddress,port);                                          /*create socket*/
          out = new PrintWriter( controlSocket.getOutputStream() , true );                       /*Write to socket*/
          in = new BufferedReader( new InputStreamReader( controlSocket.getInputStream() ) );    /*read from socket*/

          System.out.println("Succesfully connected to " + inetAddress + " on port " + port);
          answer = in.readLine();
      }
      catch(UnknownHostException e){
          System.err.println("Couldn't reach host:" + inetAddress);
          return (false);
      }
      catch(IOException e){
          System.err.println("Ioexception occured when trying to create socket I/O");
          return (false);
      }

      return(true);

  }

  /*User interface for loging onto server*/

  public void loginUI() {
    String username, passwd;
    String socketInput;

    try {
      System.out.print("Login Username: ");
      username = reader.readLine();
      System.out.print("Login Password: ");
      passwd = reader.readLine();

      if( login(username, passwd) )
        System.out.println("Login for user \""+username+"\" OK!");
      else
        System.out.println("Login for user \""+username+"\"Failed!");

    } catch(IOException ex) {
      ex.printStackTrace();
    }
  }

  /*Actual login method*/

  public boolean login(String username, String password) {

      String answer;

      out.println("USER " + username);
      out.flush();

      try{
          answer = in.readLine();
      }
      catch(IOException ex){
          System.out.println("Error while reading from Socket");
          return(false);
      }

      out.println("PASS " + password);
      out.flush();

      try{
          answer = in.readLine();
      }
      catch(IOException ex){
          System.out.println("Error while reading from Socket");
          return(false);
      }

      if( answer.charAt(0) == '2'){       /*Succesfull login*/
          return (true);
      }
      else{   /*Something went wrong*/
          return (false);
      }
  }

  /*List user interface*/

  public void listUI() {
    try {
      System.out.print("Enter path to list (or . for the current directory): ");
      String path = reader.readLine();
      String info = list(path);

      if(info == "error"){
          System.out.println("LIST failed!");
          return;
      }

      List<RemoteFileInfo> list = parse(info);
      for(RemoteFileInfo listinfo : list)
        System.out.println(listinfo);
    } catch(IOException ex) {
      ex.printStackTrace();
      System.exit(-1);
    }
  }

  /*Method for implementating list method*/

  public String list(String path) {

      String[] connectionData = new String[2];
      String answer = null;
      String listResult = null;
      String parentDir = null;

      /*Get hostname and port for second connection, using PASV*/
      connectionData = pasvSetup();

      /*New thread that will receive the LIST output*/
      ListThread infoThread = new ListThread( connectionData[0] , Integer.parseInt( connectionData[1] , 16 ) );

      /*Send TYPE A to get ascii connection*/
      out.println("TYPE A");
      out.flush();

      /*Read answer*/
      try{
          answer = in.readLine();
      }
      catch(IOException ex){
          System.out.println("Error while reading from Socket");
          System.exit(-1);
      }


      /*Send LIST message*/
      out.println("LIST " + path );
      out.flush();

      /*Read answer*/
      try{
          answer = in.readLine();
      }
      catch(IOException ex){
          System.out.println("Error while reading from Socket");
          System.exit(-1);
      }

      if( answer.charAt(0) == '5'){
          return "error";
      }
      else{
          infoThread.start();       /*Start thread*/
          try{
              infoThread.join();    /*Wait for thread to finish*/
          }
          catch(InterruptedException e){
              System.out.println("Thread was interrupted");
              System.exit(-1);
          }
      }

      /*Read answer*/
      try{
          answer = in.readLine();
      }
      catch(IOException ex){
          System.out.println("Error while reading from Socket");
          System.exit(-1);
      }

      /*Since in remotefileinfo the parent directory is required, we have to add it in the info returned*/

      String currentDir = pwd();                               /*get current directory*/
      cwd(path);                                               /*Change to target directory*/
      String targetDirectory = pwd();                          /*Print target directory*/

      /*Now get name of parent Directory*/
      int end = targetDirectory.lastIndexOf("/");
      if( targetDirectory.indexOf("/") != end ){
          int start = end - 1;
          while( targetDirectory.charAt(start) != '/'){
              start--;
          }
          start++;                                                  /*Fix position*/
          parentDir = targetDirectory.substring(start, end);
      }
      else{
          if(targetDirectory.length() > 1){
              parentDir = targetDirectory.substring(end + 1);
          }
          else{
              parentDir = "none";
          }
      }

      cwd(currentDir);                                          /*Go back to original directory*/

      return(parentDir + "\n" + infoThread.getString());

  }

  /*Code for the thread which will be used for receiving LISt output*/

  class ListThread extends Thread{

      private Socket fileSocket;
      private BufferedReader socketIn;
      private String listResult = "";

      /*Creates socket and socket reader*/
      public ListThread(String hostName, int port){

          try{
              fileSocket = new Socket(hostName, port);
              socketIn = new BufferedReader(new InputStreamReader( fileSocket.getInputStream() ) );
          }
          catch(IOException e){
              System.err.println("Error when creating file Socket");
              System.err.println("HOSTNAME IS: " + hostName + " PORT IS: " + port );
              System.exit(-1);
          }
      }

      /*Get data from second connection*/
      public void run(){

          String line = null;

          try{
              while( (line = socketIn.readLine()) != null){
                  listResult = listResult + line + "\n" ;
              }
          }
          catch(IOException e){
              System.err.println("Error when reading from socket");
              System.exit(-1);
          }

          try{
              fileSocket.close();
              socketIn.close();
          }
          catch(IOException e){
              System.err.println("Error when closing fileSocket resources");
              System.exit(-1);
          }

      }

      public String getString(){
          return listResult;
      }

  }

  /*This method can be used from methods that require a second connection to the server*/

  private String[] pasvSetup(){

      String answer = null;
      String[] data = new String[2];
      int msByte,lsByte ;          /*Most significant byte and least significant */

      /*Requesting socket for second connection*/
      out.println("PASV");
      out.flush();

      try{
          answer = in.readLine();
      }
      catch(IOException ex){
          System.out.println("Error while reading from Socket");
          System.exit(-1);
      }

      /*processing answer from server*/
      /*address is stored in data[0] , port number is stored in data[1] */
      /*First get inetAddress*/

      /*Get index of last character to read from answer, so that the inetAddress will be correctly read*/
      int separator = answer.indexOf(")");
      int commaCounter = 0;

      while(commaCounter < 2){
          separator --;
          if( answer.charAt(separator) == ','){
              commaCounter++;
          }
      }

      data[0] = answer.substring( answer.indexOf("(") + 1 , separator );
      data[0] = data[0].replace( ',' , '.' );

      /*Then get socket number*/

      data[1] = answer.substring( separator + 1 , answer.indexOf(")") );/*separator can be used again here*/

      /*Get individual integers from answer*/
      msByte = Integer.parseInt( data[1].substring( 0 , data[1].indexOf(",")) );
      lsByte = Integer.parseInt( data[1].substring( data[1].indexOf(",") + 1) );

      /*Get hexadecimal String and combine both to one String*/
      String msByteString = Integer.toHexString(msByte);
      String lsByteString = Integer.toHexString(lsByte);

      if(msByteString.length() == 1){
          msByteString =  "0" + msByteString ;
      }
      if(lsByteString.length() == 1){
          lsByteString =  "0" + lsByteString ;
      }

      data[1] = msByteString + lsByteString;

      return data;

  }

  class RemoteFileInfo {
    public boolean dir = false; // is directory
    boolean ur = false;  // user read permission
    boolean uw = false;  // user write permission
    boolean ux = false;  // user execute permission
    boolean gr = false;  // group read permission
    boolean gw = false;  // group write permission
    boolean gx = false;  // group execute permission
    boolean or = false;  // other read permission
    boolean ow = false;  // other write permission
    boolean ox = false;  // other execute permission
    public long size;           // file size
    public String name;
    String parentDir;
    String rawLine = null;         //in case it can be processed this will be printed
    boolean legit;

    public RemoteFileInfo(String line, String parent) {

        permissions( line.substring(0,10) );    /*Set up permissions*/

        int end,start;

        /*Now get parent directory*/
        parentDir = parent;

        /*Line cannot pe processed*/
        if( line.indexOf(":") == -1 || (line.indexOf(":") != line.lastIndexOf(":"))){
            rawLine = line;
            legit = false;
        }
        else if( line.indexOf(":") == line.lastIndexOf(":") ){   /*Line can be processed correctly*/
            end = line.indexOf(":");
            end --;
            while( Character.isDigit( line.charAt(end) ) || line.charAt(end) == ' '){   /*Will Stop at month*/
                end--;
            }
            end -= 3;   /*Skip monthname*/

            start = end - 1;
            while( Character.isDigit( line.charAt(start) ) ){
                start--;
            }

            start++;/*Correct starting position*/

            size = Integer.parseInt( line.substring( start, end ) );

            /*Now get filename*/
            start = line.indexOf(":") + 4;      /*Right after the time ends plus one space character*/
            name = line.substring(start);

            legit = true;
        }
    }

    /*Gets the permissions string and processes it*/
    private void permissions(String perms) {

        dir = perms.charAt(0) == 'd';
        ur = perms.charAt(1) == 'r';
        uw = perms.charAt(2) == 'w';
        ux = perms.charAt(3) == 'x';
        gr = perms.charAt(4) == 'r';
        gw = perms.charAt(5) == 'w';
        gx = perms.charAt(6) == 'x';
        or = perms.charAt(7) == 'r';
        ow = perms.charAt(8) == 'w';
        ox = perms.charAt(9) == 'x';

    }

    public String toString() {

        if(legit){
            return  "Name: " + name + "\n" +
                    "Size: " + size + "\n" +
                    "Parent Directory: " + parentDir + "\n" +
                    "Is a Directory: " + dir + "\n" +
                    "User read-write-execute permissions: " + ur + " " + uw + " " + ux + "\n" +
                    "Group read-write-execute permissions: " + gr + " " + gw + " " + gx + "\n" +
                    "Other read-write-execute permissions: " + or + " " + ow + " " + ox + ".\n" ;
        }
        else{
            return  rawLine.substring(11) + "\n" +
            "User read-write-execute permissions: " + ur + " " + uw + " " + ux + "\n" +
            "Group read-write-execute permissions: " + gr + " " + gw + " " + gx + "\n" +
            "Other read-write-execute permissions: " + or + " " + ow + " " + ox + ".\n" ;
        }
    }
 }

  public List<RemoteFileInfo> parse(String info) {

      ArrayList<RemoteFileInfo> infoList = new ArrayList<RemoteFileInfo>();     /*List to be returned*/
      String parentDir = info.substring( 0 , info.indexOf("\n") );              /*Get parent directory*/

      info = info.substring( info.indexOf("\n") + 1);                              /*Remove first line*/
      info = info.substring( info.indexOf("\n") + 1);                              /*Remove "Total XX" line*/

      /*read from String*/
      BufferedReader stringReader = new BufferedReader(new StringReader (info) );
      String line = null;

      /*Create list*/
      try{
          while( ( line = stringReader.readLine() ) != null){
              infoList.add( new RemoteFileInfo(line,parentDir) );
          }
      }
      catch(IOException ex){
          System.err.println("Error occurred when reading from info string");
          System.exit(-1);
      }

      return infoList;

  }

  public void uploadUI() {
    try {
      System.out.print("Enter file to upload: ");
      String filepath = reader.readLine();
      File file = new File(filepath);
      mupload(file);
    } catch(IOException ex) {
      ex.printStackTrace();
      System.exit(-1);
    }
  }

  /*Simple upload */
  public void upload(File f) {

      String answer = null;
      UploadThread uploadT;
      String[] hostAndPort = new String[2];

      /*Send message to get binary type connection*/
      out.println("TYPE I");
      out.flush();

      /*Get answer*/
      try{
          answer = in.readLine();
      }
      catch(IOException ex){
          System.out.println("Error while reading from Socket");
          System.exit(-1);
      }

      hostAndPort = pasvSetup();

      uploadT = new UploadThread(hostAndPort[0], Integer.parseInt(hostAndPort[1] , 16) ,f);

      /*Send store message*/
      out.println("STOR " + f.getName());
      out.flush();

      /*Get answer*/
      try{
          answer = in.readLine();
      }
      catch(IOException ex){
          System.out.println("Error while reading from Socket");
          System.exit(-1);
      }

      uploadT.run();

      try{
          uploadT.join();
      }
      catch(InterruptedException ex){
          System.err.println("Thread got interrupted");
          System.exit(-1);
      }

  }

  /**
   * Upload multiple files
   * @param f can be either a local filename or local directory
   */
  public void mupload(File f) {

      String answer = null;
      UploadThread uploadT;
      String[] hostAndPort = new String[2];

      /*File doesn't exist in local directory*/
      if(!f.exists()){
          System.out.println("File \"" + f.getName() + "\" does not exist");
          return;
      }

      if(f.isFile()){
          upload(f);
      }
      else{
          if( !mkdir(f.getName()) ){             //file exists on server
              System.out.println("Couldnt create directory");
              return;
          }
          System.out.println("Created directory \"" + f.getName() + "\"");
          cwd(f.getName());
          File[] fileEntries = f.listFiles();
          for( File entry : fileEntries){
              mupload(entry);
          }
          cwd("..");                    //Go back to parentDir
      }

  }

  class UploadThread extends Thread{

      private String answer;
      private Socket fileSocket;
      private OutputStream socketOut;
      private BufferedInputStream fileReader;
      private File upFile;

      public UploadThread(String host, int port,File uploadFile){

          /*Set up connection and streams*/
          upFile = uploadFile;
          try{
              fileSocket = new Socket(host, port);
              socketOut = fileSocket.getOutputStream();
              fileReader = new BufferedInputStream( new FileInputStream(upFile));
          }
          catch(IOException ex){
            System.err.println("Opening port");
            System.exit(-1);
          }

      }

      /*Use for uploading one file*/
      public void run(){

          try{
              byte[] fileData = new byte[(int)upFile.length()];             /*File data*/
              fileReader.read(fileData,0,fileData.length);                  /*Read data from File*/
              socketOut.write(fileData,0,fileData.length);
              socketOut.flush();
              socketOut.close();
              fileReader.close();
              fileSocket.close();
          }
          catch(IOException ex){
              System.out.println("Error while reading from Socket");
              System.exit(-1);
          }
          /*After Sending file get answer from controlsocket*/
          try{
              answer = in.readLine();
          }
          catch(IOException ex){
              System.out.println("Error while reading from Socket");
              System.exit(-1);
          }

          System.out.println("Uploaded \"" + upFile.getName() + "\"");

      }

  }

  /*User interface for download*/

  public void downloadUI() {
    try {
      System.out.print("Enter file to download: ");
      String filename = reader.readLine();
      File file = new File(filename);                /* we have an absolute path */

      if( file.exists() && !file.isDirectory()) {
        System.out.println("File \""+file.getPath()+"\" already exists.");
        String yesno;
        do {
          System.out.print("Overwrite (y/n)? ");
          yesno = reader.readLine().toLowerCase();
        } while( !yesno.startsWith("y") && !yesno.startsWith("n") );
        if( yesno.startsWith("n") )
          return;
      }

      /*Get info of directory to see if name given is directory or file*/
      String info = list(".");
      List<RemoteFileInfo> files = parse(info);
      RemoteFileInfo correctFile = null;
      for(RemoteFileInfo remote : files){   /*Iterate over list*/
          if( remote.name.equals(filename) ){      /*Found it*/
              correctFile = remote;
              break;
          }
      }

      /*check if file exists on server*/
      if(correctFile == null){
          System.out.println("No such file found");
          return;
      }
      else if(!correctFile.dir){
          download(correctFile,file);
      }
      else{
          mdownload(correctFile,null);
      }

    } catch(IOException ex) {
      ex.printStackTrace();
      System.exit(-1);
    }
  }

  /**
   * Download multiple files
   * @param entry can be either a filename or directory
   */
  public boolean mdownload(RemoteFileInfo entry, String curPath){

      if(curPath == null){
          curPath = entry.name;
      }
      else{
          curPath = curPath + "/" + entry.name;
      }

      cwd(entry.name);
      File local = new File(curPath);
      local.mkdir();
      System.out.println("Created directory \"" + entry.name + "\"");
      List<RemoteFileInfo> list = parse( list(".") );
      for(RemoteFileInfo listentry : list) {
          if( !listentry.dir ){
              File dFile = new File(curPath + "/" + listentry.name);
              download(listentry,dFile);
          }
          else{
              mdownload(listentry,curPath);
          }
      }
      cwd("..");
      if( curPath.contains("/") ){
          curPath = curPath.substring(curPath.lastIndexOf('/'));
      }

      return true;

  }

  /**
   * Return values:
   *  0: success
   * -2: download failure
   */
  public int download(RemoteFileInfo entry,File downFile) {

      String[] connectionData = new String[2];
      String answer = null;

      out.println("TYPE I");                /*Inform server about type of connection*/
      out.flush();

      /*Read message from controlSokcet*/
      try{
          answer = in.readLine();
      }
      catch(IOException e){
          System.out.println("Error when reading from socket");
          System.exit(-1);
      }

      if(answer.charAt(0) != '2'){
          System.out.println("Download failed!");
          return -2;
      }

      connectionData = pasvSetup();

      /*Create Thread*/
      DownloadThread dThread = new DownloadThread(connectionData[0],
                                    Integer.parseInt(connectionData[1],16),downFile,entry.size);

      /*Send store message*/
      out.println("RETR " + downFile.getName());
      out.flush();


      /*Get answer*/
      try{
          answer = in.readLine();
      }
      catch(IOException ex){
          System.out.println("Error while reading from Socket");
          System.exit(-1);
      }

      /*Start thread*/
      dThread.run();
      try{
          dThread.join();
      }
      catch(InterruptedException ex){
          System.out.println("DownloadThread got interrupted");
          System.exit(-1);
      }

      /*Get answer*/
      try{
          answer = in.readLine();
      }
      catch(IOException ex){
          System.out.println("Error while reading from Socket");
          System.exit(-1);
      }

      /*Check if download failed*/
      if( dThread.success == false){
          System.out.println("Download failed!");
          return -2;
      }

      return 0;

  }

  /*Thread that will be created for downloading files*/
  class DownloadThread extends Thread{

      private String fileName;
      private Socket fileSocket;
      private InputStream socketIn;
      private BufferedOutputStream fileWriter;
      private long fileSize;
      private File downFile;
      public boolean success = true;
      private byte[] fileInfo;

      public DownloadThread(String host, int port, File file, long fileSize){

          /*Set up connection and streams*/
          downFile = file;
          fileName = file.getName();
          this.fileSize = fileSize;
          try{
              fileSocket = new Socket(host, port);
              socketIn = fileSocket.getInputStream();
              fileWriter = new BufferedOutputStream(new FileOutputStream(downFile));
          }
          catch(IOException ex){
            System.err.println("Opening port error");
            System.exit(-1);
          }
      }

      public void run(){

          /*Buffer to store information, check if filesize is greater than maximum integer, for array creation*/
          if(fileSize > Integer.MAX_VALUE){
              fileInfo = new byte[Integer.MAX_VALUE];
          }
          else{
              fileInfo = new byte[(int)fileSize];
          }

          try{
              int bytesRead = socketIn.read(fileInfo,0,fileInfo.length);        /*read data*/
              int cur = bytesRead;                                                            /*read data until no more*/
              bytesRead = socketIn.read(fileInfo, cur, (fileInfo.length-cur));
              if(bytesRead >= 0){
                cur += bytesRead;
              }
              /*Array is full, case where filzesize > MAX VALUE of int, so write what was read and start reading again*/
              if(cur == Integer.MAX_VALUE - 1){
                  fileWriter.write(fileInfo,0,cur);
                  fileWriter.flush();
                  cur = 0;
              }

              fileWriter.write(fileInfo,0,cur);                         /*Write data to file*/
              fileWriter.flush();
          }
          catch(IOException ex){
              System.err.println("Error when reading from socket or writing to file");
              success = false;
              return;
          }
          System.out.println("Download of file " + fileName + " completed.");

      }

  }

/*Make directory method*/

  public boolean mkdir(String dirname) {

      String answer = null;

      out.println("MKD " + dirname);
      out.flush();

      try{
          answer = in.readLine();
      }
      catch(IOException e){
          System.out.println("Error when reading from socket");
          System.exit(-1);
      }

      if( answer.charAt(0) == '2'){                 /*Successfull*/
          return true;
      }
      else{
          return false;
      }


  }

/*User interface for make directory method*/

  public void mkdirUI() {
    String dirname, socketInput;
    try {
      System.out.print("Enter directory name: ");
      dirname = reader.readLine();

      if( mkdir(dirname) )
        System.out.println("Directory \""+ dirname +"\" created!" );
      else
        System.out.println("Directory creation failed!");
    } catch(IOException ex) {
      ex.printStackTrace();
    }
  }

  /*Remove directory method*/

  public boolean rmdir(String dirname) {

      String answer = null;

      out.println("RMD " + dirname);
      out.flush();

      try{
          answer = in.readLine();
      }
      catch(IOException e){
          System.out.println("Error when reading from socket");
          System.exit(-1);
      }

      //System.out.println(answer);

      if( answer.charAt(0) == '2'){                 /*Successfull*/
          return true;
      }
      else{
          return false;
      }

  }

  /*User interface for remove directory method*/

  public void rmdirUI() {
    String dirname, socketInput;
    try {
      System.out.print("Enter directory name: ");
      dirname = reader.readLine();

      if( rmdir(dirname) )
        System.out.println("Directory \""+ dirname +"\" deleted!" );
      else
        System.out.println("Directory deletion failed!");
    } catch(IOException ex) {
      ex.printStackTrace();
    }
  }

  public void deleteUI() {
    String filename, socketInput;
    String answer;

    try {
      System.out.print("Enter file to delete: ");
      filename = reader.readLine();
      File file = new File(filename);

      List<RemoteFileInfo> list = parse( list(filename) );
      if( list.size() > 1 || list.size()==0 || !list.get(0).name.equals(filename) ) {
        File filepath = file.getParentFile() != null ? file.getParentFile() : new File(".");
        list = parse( list( filepath.getPath() ) );
        boolean found = false, deleted = false;
        for(RemoteFileInfo entry : list)
          if( entry.name.equals(filename) ) {
            found = true;
            if( mdelete( entry ) ) {
              deleted = true;
            }
          }
          if(found && deleted)
            System.out.println("Filename \""+filename+"\" deleted successfully");
          else if( !found )
            System.out.println("Unable to find \""+filename+"\"");
          else if( !deleted )
            System.out.println("Failed to delete \""+filename+"\"");

      }
      else if( list.size() == 1 ) {
        for(RemoteFileInfo entry : list) {
          if( !mdelete( entry ) ) {
            System.out.println("Failed to delete filename \""+entry.name+"\"");
            return;
          }
          System.out.println("Filename \""+entry.name+"\" deleted successfully");
        }
      }

    } catch(IOException ex) {
      ex.printStackTrace();
      System.exit(-1);
    }

  }

  /* Delete multiple files in case entry is a directory
   */
  public boolean mdelete(RemoteFileInfo entry) {
    if( entry.dir ) {
      cwd( entry.name );
      List<RemoteFileInfo> list = parse( list(".") );
      for(RemoteFileInfo listentry : list) {
        mdelete(listentry);
      }
      cwd("..");
      if( !rmdir( entry.name ) ) {
        System.out.println("Deletion of directory \"" + entry.name + "\" failed!");
        return false;
      }
      else{
          System.out.println("Deleted directory \"" + entry.name + "\"");
      }
      return true;
    }
    else {
      if( !delete( entry.name ) ) {
        System.out.println("Deletion of file \""+entry.name+"\" failed!");
        return false;
      }
      else{
          System.out.println("Deleted file \"" + entry.name + "\"");
      }
      return true;
    }
  }

  public boolean delete(String filename) {

    String answer = null;

    out.println("DELE " + filename);
    out.flush();

    try{
        answer = in.readLine();
    }
    catch(IOException e){
        System.out.println("Error when reading from socket");
        System.exit(-1);
    }

    //System.out.println(answer);

    if(answer.charAt(0) == '2'){
        return true;
    }
    else{
        return false;
    }

  }

/*User interface for change working directory method*/

  public void cwdUI() {
    String dirname, socketInput;
    try {
      System.out.print("Enter directory name: ");
      dirname = reader.readLine();
      dbg(null, "Read: "+dirname);

      if( cwd(dirname) )
        System.out.println("Directory changed successfully!");
      else
        System.out.println("Directory change failed!");
    } catch(IOException ex) {
      ex.printStackTrace();
    }
  }

/*Implementation of change working directory*/

  public boolean cwd(String dirname) {

      String answer = null;

      out.println("CWD " + dirname);
      out.flush();

      try{
          answer = in.readLine();
      }
      catch(IOException e){
          System.out.println("Error when reading from socket");
          System.exit(-1);
      }

      if(answer.charAt(0) == '2'){
          return true;
      }
      else{
          return false;
      }

  }

/*User interface for print working directory method*/

  public void pwdUI() {

    String dirname, socketInput;
    String pwdInfo = pwd();

    if(pwdInfo != null){
        System.out.println("PWD: "+pwdInfo);
    }
    else{
        System.out.println("Pwd failed!");
    }

  }

/*Implementation of print working directory*/

  public String pwd() {

      String answer = null;

      out.println("PWD");
      out.flush();

      try{
          answer = in.readLine();
      }
      catch(IOException e){
          System.out.println("Error when reading from socket");
          System.exit(-1);
      }

      if(answer.charAt(0) == '2'){    /*success*/
          return ( answer.substring( answer.indexOf('"') + 1 , answer.lastIndexOf('"') ) );
      }
      else{               /*Error*/
          return null;
      }

  }

  public void renameUI() {
    try {
      System.out.print("Enter file or directory to rename: ");
      String from = reader.readLine();
      System.out.print("Enter new name: ");
      String to = reader.readLine();

      if( rename(from, to) )
        System.out.println("Rename successfull");
      else
        System.out.println("Rename failed!");
    } catch(IOException ex) {
      ex.printStackTrace();
      return;
    }
  }

  public boolean rename(String from, String to) {

      String answer = null;

      /*First specify which file to rename*/

      out.println("RNFR " + from);
      out.flush();

      try{
          answer = in.readLine();
      }
      catch(IOException e){
          System.out.println("Error when reading from socket");
          System.exit(-1);
      }


      if( answer.charAt(0) == '5'){
          return false;
      }

      /*Then specify new name*/

      out.println("RNTO " + to);
      out.flush();

      try{
          answer = in.readLine();
      }
      catch(IOException e){
          System.out.println("Error when reading from socket");
          System.exit(-1);
      }

      if( answer.charAt(0) == '2'){
          return true;
      }
      else{
          return false;
      }
  }

  public void helpUI() {
    System.out.println("OPTIONS:\n\tLOGIN\tQUIT\tLIST\tUPLOAD\tDOWNLOAD\n\tMKD\tRMD\tCWD\tPWD\tDEL\n");
    help();
  }

  public void checkInput(String command) {
    switch(command.toUpperCase()) {
    case "HELP" :
      helpUI();
      break;
    case "CONNECT" :
      bindUI(null);
      break;
    case "LOGIN" :
      loginUI();
      break;
    case "UPLOAD" :
      uploadUI();
      break;
    case "DOWNLOAD" :
      downloadUI();
      break;
    case "CWD" :
    case "CD" :
      cwdUI();
      break;
    case "PWD" :
      pwdUI();
      break;
    case "LIST" :
      listUI();
      break;
    case "MKD" :
    case "MKDIR" :
      mkdirUI();
      break;
    case "RMD" :
    case "RMDIR" :
      rmdirUI();
      break;
    case "DEL" :
    case "DELE" :
    case "DELETE" :
    case "DLT" :
      deleteUI();
      break;
    case "RENAME":
    case "RNM":
      renameUI();
      break;
    case "QUIT" :
      System.out.println("Bye bye...");
      System.exit(1);
      break;
    default :
      System.out.println("ERROR: Unknown command \""+command+"\"");
    }
  }

  /*Method to print usage and valid commands for the ftp client*/
  public static void help(){

      System.out.println("Usage of ftp client commands:\n");
      System.out.println("HELP                            List available commands");
      System.out.println("LOGIN                           Login to server ");
      System.out.println("LIST                            List directory contents");
      System.out.println("CWD                             Change working directory");
      System.out.println("PWD                             Print working directory");
      System.out.println("MKD                             Create directory");
      System.out.println("RMD                             Remove directory");
      System.out.println("DLT                             Delete specified file or directory");
      System.out.println("UPLOAD                          Upload specified local file or directory");
      System.out.println("DOWNLOAD                        Download specified file or directory");
      System.out.println("RENAME                          Rename a file or directory");
      System.out.println("QUIT                            Terminate programm");

  }

  public static void main(String [] args) {
    FtpClient client = new FtpClient(true, true);
    client.bindUI(args);
    client.loginUI();
    System.out.print("$> ");
    try {
      String userInput;
      while( true ) {
        if( client.reader.ready() ) {
          userInput = client.reader.readLine();
          while( userInput.indexOf(' ') == 0 ) {
            userInput = userInput.substring(1);
          }
          if( userInput.indexOf(' ') < 0 ) {
            client.checkInput(userInput);
          }
          if( userInput.indexOf(' ') > 0 ) {
            client.checkInput( userInput.substring(0, userInput.indexOf(' ')) );
          }
          System.out.print("$> ");
       }
       else {
         Thread.sleep(500);
       }
      }
    }
    catch(Exception ex) {
      ex.printStackTrace();
    }
  }

}
