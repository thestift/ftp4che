/*
 * Created on 11.06.2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.ftp4che;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.List;



import org.apache.log4j.Logger;
import org.ftp4che.commands.Command;
import org.ftp4che.commands.ListCommand;
import org.ftp4che.exception.ConfigurationException;
import org.ftp4che.exception.NotConnectedException;
import org.ftp4che.exception.UnkownReplyStateException;
import org.ftp4che.reply.Reply;
import org.ftp4che.util.FTPFile;
import org.ftp4che.util.ReplyFormatter;
import org.ftp4che.util.ReplyWorker;
import org.ftp4che.util.SocketProvider;


/**
 * @author arnold,kurt
 *
 */
public abstract class FTPConnection {
    
    //TODO: support PRET command
    
    
    /* Constants for connection.type*/
    public static final int FTP_CONNECTION = 1;
    public static final int IMPLICIT_SSL_FTP_CONNECTION =  2;
    public static final int AUTH_SSL_FTP_CONNECTION =  3;
    public static final int AUTH_TLS_FTP_CONNECTION =  4;
  
    /* Connection status that are possbile */
    
    public static final int CONNECTED = 1001;
    public static final int DISCONNECTED = 1002;
    public static final int IDLE = 1003;
    public static final int RECEIVING_FILE = 1004;
    public static final int SENDING_FILE = 1005;
    public static final int FXP_FILE = 1006;
    public static final int UNKNOWN = 9999;
    
    
    /* Member variables 
     */
    Logger log = Logger.getLogger(FTPConnection.class.getName());
    InetSocketAddress address = null;
    String user = "";
    String password = "";
    String account = "";
    boolean passiveMode = false;
    int timeout = 10000;
//  Charset and decoder
    Charset charset = Charset.forName("ISO-8859-1");
    CharsetDecoder decoder = charset.newDecoder();
    CharsetEncoder encoder = charset.newEncoder();
    // Direct byte buffer for reading
    
    //TODO: make configurable 
    ByteBuffer downloadBuffer = ByteBuffer.allocateDirect(65536);
    ByteBuffer uploadBuffer = ByteBuffer.allocateDirect(8192);
    CharBuffer controlBuffer = CharBuffer.allocate(4096);
    protected SocketProvider socketProvider = null;
    
    /**
     * @author arnold,kurt
     * @param address Set method for the address the FTPConnection will connect to if connect() is called
     */
     public InetSocketAddress getAddress() {
        return address;
    }
    /**
     * @param address The address to set.
     */
    public void setAddress(InetSocketAddress address)
    {
        this.address = address;
    } 
 
    /**
     * @author arnold,kurt
     * @param password Get method for the password the FTPConnection will use if connect() is called
     */
    public String getPassword() {
        return password;
    }
    /**
     * @author arnold,kurt
     * @param password Set method for the password the FTPConnection will use if connect() is called
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * @author arnold,kurt
     * @param user Get method for the user the FTPConnection will use if connect() is called
     * @throws ConfigurationException will be thrown if a parameter is missing or invalid
     */
    public String getUser() {
        return user;
    }
    
    /**
     * @author arnold,kurt
     * @param user Set method for the user the FTPConnection will use if connect() is called
     * @throws ConfigurationException will be thrown if a parameter is missing or invalid
     */
    public void setUser(String user) throws ConfigurationException {
        if(user == null || user.length() == 0)
            throw new ConfigurationException("user must no be null or has a length of 0");
        this.user = user;
    }
 
 
    
    /**
     * @author arnold,kurt
     * @param account Get method for the account the FTPConnection will use if connect() is called
     */
    public String getAccount() {
        return account;
    }
    /**
     * @author arnold,kurt
     * @param account Set method for the account the FTPConnection will use if connect() is called
     */
    public void setAccount(String account) {
        this.account = account;
    }
    
    
    /**
     * This method is used to connect and login to the specified server.
     * @author arnold,kurt
     * @exception NotConnectedException will be thrown if it was not possible to establish a connection to the specified server
     * @exception IOException will be thrown if it there was a problem sending the LoginCommand to the server
     */
    public abstract void connect() throws NotConnectedException,IOException;
    
    /**
     * This method is used to disconnect from the specified server.
     * @author arnold,kurt
    */
    public void disconnect() {
        try
        {
            Command command = new Command(Command.QUIT);
            sendCommand(command);
        }catch (IOException ioe)
        {
          log.warn("Error closing connection: " + getAddress().getHostName() + ":" + getAddress().getPort(),ioe);
          socketProvider = null;
        }
         
     }
    
    /**
     * This method is used to send commands (there is an implementation for each possible command).
     * You should call this method if you want to send a raw command and get the full results or if there is no implemented corresponding method.
     * @return Reply for the specific command.
     * You will get a result for each server reply. 
     * @author arnold,kurt
     * @exception IOException will be thrown if there was a communication problem with the server
    */
    public Reply sendCommand(Command cmd) throws IOException{
        controlBuffer.clear();
        log.debug("Sending command: " + cmd.toString());
        controlBuffer.put(cmd.toString());
        controlBuffer.flip();
        socketProvider.write(encoder.encode(controlBuffer));
        controlBuffer.clear();
        return ReplyWorker.readReply(socketProvider);
     }
    
    /**
     * 
     * This method is used to get the status of your connection
     * @return status there are constants in FTPConnection (f.e. CONNECTED / DISCONNECTED / IDLE ...) where you can identify the status of your ftp connection
     * @author arnold,kurt
     */
    public int getConnectionStatus()
    {
        //TODO: IMPLEMENT
        return FTPConnection.CONNECTED;
    }
    
    /**
     * This method is used initaly to set the connection timeout. normal you would set it to 10000 (10 sec.). if you have very slow servers try to set it higher.
     * @param millis the milliseconds before a timeout will close the connection
     * @author arnold,kurt
     */
    public void setTimeout(int millis) {
        this.timeout = millis;
    }
    
    /**
     * This method is used initaly to get the connection timeout. normal you would set it to 10000 (10 sec.). if you have very slow servers try to set it higher.
     * @param millis the milliseconds before a timeout will close the connection
     * @author arnold,kurt
     */
    public int getTimeout()
    {
        return timeout;
    }
    
    /**
     * This method is used to change the working directory. it implements the CWD ftp command
     * @param directory a string represanting the new working directory
     * @author arnold,kurt  
     * @throws IOException will be thrown if there was a communication problem with the server
     */
    public void changeDirectory(String directory) throws IOException
    {
        Command command = new Command(Command.CWD,directory);
        (sendCommand(command)).dumpReply(System.out);
    }
    
    /**
     * This method is used to get the working directory. it implements the PWD ftp command
     * @author arnold,kurt
     * @throws IOException  will be thrown if there was a communication problem with the server
     */
    public String getWorkDirectory() throws IOException,UnkownReplyStateException
    {
        Command command = new Command(Command.PWD);
        Reply reply = sendCommand(command);
        reply.dumpReply(System.out);
        return ReplyFormatter.parsePWDReply(reply);
    }
    
    /**
     * This method is used to change to the parent directory. it implements the CDUP ftp command
     * @author arnold,kurt
     * @throws IOException  will be thrown if there was a communication problem with the server
     */
    public void changeToParentDirectory() throws IOException
    {
        Command command = new Command(Command.CDUP);
        (sendCommand(command)).dumpReply(System.out);
    }
    
    /**
     * This method is used to create a new directory. it implements the MKD ftp command
     * @param pathname a string represanting the directory to create
     * @author arnold,kurt
     * @throws IOException will be thrown if there was a communication problem with the server
     */
    public void makeDirectory(String pathname) throws IOException
    {
        Command command = new Command(Command.MKD,pathname);
        (sendCommand(command)).dumpReply(System.out);
    }

    /**
     * This method is used to remove a specific directory. it implements the RMD ftp command
     * @param pathname a string represanting the directory to remove
     * @author arnold,kurt
     * @throws IOException will be thrown if there was a communication problem with the server
     */
    public void removeDirectory( String pathname ) throws IOException
    {
        Command command = new Command(Command.RMD,pathname);
        (sendCommand(command)).dumpReply(System.out);
    }
    
    /**
     * This method is used to send a noop comand to the server i.g. for keep alive purpose. 
     * it implements the NOOP ftp command
     * @author arnold,kurt
     * @throws IOException will be thrown if there was a communication problem with the server
     */
    public void noOperation() throws IOException
    {
        Command command = new Command(Command.NOOP);
        (sendCommand(command)).dumpReply(System.out);
    }
    
    /**
     * This method is used to go into passive mode. it implements the PASV ftp command
     * @author arnold,kurt
     * @throws IOException will be thrown if there was a communication problem with the server
     */
    public InetSocketAddress sendPassiveMode() throws IOException
    {
    	Command command = new Command(Command.PASV);
    	try
    	{
    		return ReplyFormatter.parsePASVCommand(sendCommand(command));
    	}catch (UnkownReplyStateException urse)
    	{
    		log.error(urse);
    	}
    	return null;
    }
    
    /**
     * This method is used initaly to set if passive mode should be used. 
     * Default it is false
     * @param passive if true it will use passive mode
     * @author arnold,kurt
     */
    public void setPassiveMode(boolean mode) {
        this.passiveMode = mode;  
      }
      
    /**
     * This method is used initaly to set if passive mode should be used. 
     * Default it is false
     * @param passive if true it will use passive mode
     * @author arnold,kurt
     */
    public boolean isPassiveMode() {
          return passiveMode;
    }
    
    
    
    public List<FTPFile> getDirectoryListing() throws IOException
    {
       return getDirectoryListing(".");
    }
    
    public List<FTPFile> getDirectoryListing(String directory) throws IOException
    {
    	InetSocketAddress dataSocket = null;
    	ListCommand command = new ListCommand(directory);
    	
        if(isPassiveMode())
         	dataSocket = sendPassiveMode();
          	//TODO: if not passive then get a active socket
        SocketProvider provider = new SocketProvider();
        provider.connect(dataSocket);
        command.setDataSocket(provider);
        //INFO response from ControllConnection is ignored
        (sendCommand(command)).dumpReply(System.out);
          return ReplyFormatter.parseListReply(command.fetchDataConnectionReply());
    }
    
    public void sendPortCommand(InetAddress inetaddress, int localport) throws IOException
    {
        
    }
}