# Java-Homework2
University of Thessaly
object-oriented programming second assignment

In this assignment you have to implement a FTP (File Transfer Protocol)
in Java , to be used from the command line of your computer. Your FTP
Client should support the following functions:
1. BIND: Create a TCP connection between you and a given host , on a given port
2. LOGIN: Using a username and a password. There is no fuction to logout from the server. You can connect again with a different combination of username/password to the same server. The logout function is the same as interrupting the connection to the server.
3. LIST(list directory contents): Display and store a list of files and directories contained in the current directory.
4. CWD(change working directory): Change the current working directory on the server you are connected to.
5. PWD(print working directory): Print the path to the current working directory.
6. MKD(make directory): Create an empty directory.
7. RMD(remove directory): Delete an empty directory.
8. DLT(delete file): Delete a file.
9. RENAME : Rename a file or directory.
10. QUIT: Process terminates.


Description of the FTP Protocol:

Some of the operations the FTP Protocol supports, are listed below:
1. Connect to a server using a username and a password.
2. Move(copy) a file from the server to the client (download) and from the client to the server (upload).
3. Send a list of a directories contents.
4. Change the current working directory.
5. Create an empty directory.
6. Delete an empty directory.
7. Delete a file.
8. Rename an existing file or directory on the server.

The FTP Protocol also assumes that a channel exists between a server and a client, used for communication between those two. The communication itself is in the form of text messages. When the FTP Protocol needs to transfer a large amount of data that can't be categorized under those text messages, it creates a second, provisional channel just for sending this amount of data and after the transfer is complete , it closes this channel. This type of channels is used for example, for downloading a file or for viewing the contents of a directory (LIST command).

There are two methods an accessory channel can be created:
1. The client sends a message containing the word PORT and a number representing the id of a socket. the client then waits for the server to connect on the specified socket.
2. The client sends a message with the word PASV to the server, to notify the latter that it wants to create an accessory channel. The server answers with a message that looks like this: 227 Entering Passive Mode (IP1,IP2,IP3,IP4,PORT-MSB,PORT-LSB). The values IP1 to IP4 are the values used by the client to connect to the server (for example 194,177,204,65 means that the client has to connect to the IP  address 194.177.204.65).                                                     The values PORT-MSB, PORT-LSB represent the hexadecimal representation of the most significant and the least significant byte of the port ID to connect to. For example the values 179,107 are analyzed as follows: 179 => 0xB3 and 107 => 0x6B hence 0xB36B => 45931. This means that the client has to connect to the port 45931.

The FTP Protocol doesn't support the transfer of multiple files and the creation/deletion of multiple files and/or directories. When uploading multiple files , only one file at a time can be uploaded and the same goes for downloads.

Last but not least the FTP Protocol supports two types of data transfers the first being simple ascii and the second being binary data transfer.
