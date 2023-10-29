/**
* Assignment 1
* Saumick Pradhan
**/

package webserver;

import java.io.*;
import java.net.*;
import java.util.regex.*;

public class FtpClient {
    final static String CRLF = "\r\n";
    private boolean DEBUG = true; // Debug Flag
    private Socket controlSocket = null;
    private BufferedReader controlReader = null;
    private DataOutputStream controlWriter = null;
    private String currentResponse;

    public FtpClient() {
    }

    public void connect(String username, String password) {
        try {
            // Establish the control socket
            controlSocket = new Socket("localhost", 21);
            controlReader = new BufferedReader(new InputStreamReader(controlSocket.getInputStream()));
            controlWriter = new DataOutputStream(controlSocket.getOutputStream());

            // Check if the initial connection response code is OK
            if (checkResponse(220)) {
                System.out.println("Successfully connected to FTP server");
            }

            // Send user name and password to ftp server
            sendCommand("USER " + username + CRLF, 331);
            sendCommand("PASS " + password + CRLF, 230);

        } catch (UnknownHostException ex) {
            System.out.println("UnknownHostException: " + ex);
        } catch (IOException ex) {
            System.out.println("IOException: " + ex);
        }
    }

    public void getFile(String file_name) {
        int data_port = 0; // Initialize the data port
        Socket data_socket = null;
        try {
            // Change to the current (root) directory first
            sendCommand("CWD" + CRLF, 250);

            // Set to passive mode and retrieve the data port number from response
            currentResponse = sendCommand("PASV" + CRLF, 227);
            data_port = extractDataPort(currentResponse);

            // Connect to the data port
            data_socket = new Socket("localhost", data_port);
            DataInputStream data_reader = new DataInputStream(data_socket.getInputStream());

            // Download the file from FTP server
            sendCommand("RETR " + file_name + CRLF, 150);

            if (checkResponse(226)) // Check if the transfer was successful
            {
                System.out.println("Downloading file: " + file_name);
                createLocalFile(data_reader, file_name); // Write data on a local file
            }

        } catch (UnknownHostException ex) {
            System.out.println("UnknownHostException: " + ex);
        } catch (IOException ex) {
            System.out.println("IOException: " + ex);
        }
    }

    public void disconnect() {
        try {
            controlReader.close();
            controlWriter.close();
            controlSocket.close();
        } catch (IOException ex) {
            System.out.println("IOException: " + ex);
        }
    }

    private String sendCommand(String command, int expected_response_code) {
        String response = "";
        try {
            // Send command to the FTP server
            controlWriter.writeBytes(command);

            // Get response from FTP server
            response = controlReader.readLine();
            if (DEBUG) {
                System.out.println("Current FTP response: " + response);
            }

            // Check the validity of the response
            if (!response.startsWith(String.valueOf(expected_response_code))) {
                throw new IOException("Bad response: " + response);
            }
        } catch (IOException ex) {
            System.out.println("IOException: " + ex);
        }
        return response;
    }

    private boolean checkResponse(int expected_code) {
        boolean response_status = true;
        try {
            currentResponse = controlReader.readLine();
            if (DEBUG) {
                System.out.println("Current FTP response: " + currentResponse);
            }
            if (!currentResponse.startsWith(String.valueOf(expected_code))) {
                response_status = false;
                throw new IOException("Bad response: " + currentResponse);
            }
        } catch (IOException ex) {
            System.out.println("IOException: " + ex);
        }
        return response_status;
    }

    private int extractDataPort(String response_line) {
        int data_port = 0;
        Pattern pattern = Pattern.compile("\\((.*?)\\)");
        Matcher matcher = pattern.matcher(response_line);
        String[] str = new String[6];
        if (matcher.find()) {
            str = matcher.group(1).split(",");
        }
        if (DEBUG) {
            System.out.println("Port integers: " + str[4] + "," + str[5]);
        }
        data_port = Integer.valueOf(str[4]) * 256 + Integer.valueOf(str[5]);
        if (DEBUG) {
            System.out.println("Data Port: " + data_port);
        }
        return data_port;
    }

    private void createLocalFile(DataInputStream dis, String file_name) {
        byte[] buffer = new byte[1024];
        int bytes = 0;
        try {
            FileOutputStream fos = new FileOutputStream(new File(file_name));
            while ((bytes = dis.read(buffer)) != -1) {
                fos.write(buffer, 0, bytes);
            }
            dis.close();
            fos.close();
        } catch (FileNotFoundException ex) {
            System.out.println("FileNotFoundException" + ex);
        } catch (IOException ex) {
            System.out.println("IOException: " + ex);
        }
    }

}
