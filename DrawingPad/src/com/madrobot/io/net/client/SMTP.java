/*******************************************************************************
 * Copyright (c) 2011 MadRobot.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *  Elton Kent - initial API and implementation
 ******************************************************************************/
package com.madrobot.io.net.client;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

import com.madrobot.io.net.client.command.ProtocolCommandListener;
import com.madrobot.io.net.client.command.ProtocolCommandSupport;
import com.madrobot.io.net.client.command.SMTPCommand;

/***
 * SMTP provides the basic the functionality necessary to implement your
 * own SMTP client. To derive the full benefits of the SMTP class requires
 * some knowledge of the FTP protocol defined in RFC 821. However, there
 * is no reason why you should have to use the SMTP class. The
 * {@link com.madrobot.io.net.client.SMTPClient} class,
 * derived from SMTP,
 * implements all the functionality required of an SMTP client. The
 * SMTP class is made public to provide access to various SMTP constants
 * and to make it easier for adventurous programmers (or those with
 * special needs) to interact with the SMTP protocol and implement their
 * own clients. A set of methods with names corresponding to the SMTP
 * command names are provided to facilitate this interaction.
 * <p>
 * You should keep in mind that the SMTP server may choose to prematurely close
 * a connection for various reasons. The SMTP class will detect a premature SMTP
 * server connection closing when it receives a
 * {@link com.madrobot.io.net.client.SMTPReply#SERVICE_NOT_AVAILABLE
 * SMTPReply.SERVICE_NOT_AVAILABLE } response to a command. When that occurs,
 * the SMTP class method encountering that reply will throw an
 * {@link com.madrobot.io.net.client.SMTPConnectionClosedException} .
 * <code>SMTPConectionClosedException</code> is a subclass of
 * <code> IOException </code> and therefore need not be caught separately, but
 * if you are going to catch it separately, its catch block must appear before
 * the more general <code> IOException </code> catch block. When you encounter
 * an {@link com.madrobot.io.net.client.SMTPConnectionClosedException} , you
 * must disconnect the connection with
 * {@link com.madrobot.io.net.client.SocketClient#disconnect disconnect() } to
 * properly clean up the system resources used by SMTP. Before disconnecting,
 * you may check the last reply code and text with {@link #getReplyCode
 * getReplyCode }, {@link #getReplyString getReplyString }, and
 * {@link #getReplyStrings getReplyStrings}.
 * <p>
 * Rather than list it separately for each method, we mention here that every
 * method communicating with the server and throwing an IOException can also
 * throw a {@link com.madrobot.io.net.client.MalformedServerReplyException} , which
 * is a subclass of IOException. A MalformedServerReplyException will be thrown
 * when the reply received from the server deviates enough from the protocol
 * specification that it cannot be interpreted in a useful manner despite
 * attempts to be as lenient as possible.
 * <p>
 * <p>
 * 
 * @see SMTPClient
 * @see SMTPConnectionClosedException
 * @see com.madrobot.io.net.client.MalformedServerReplyException
 ***/

public class SMTP extends SocketClient {
	/*** The default SMTP port (25). ***/
	public static final int DEFAULT_PORT = 25;

	// We have to ensure that the protocol communication is in ASCII
	// but we use ISO-8859-1 just in case 8-bit characters cross
	// the wire.
	private static final String __DEFAULT_ENCODING = "ISO-8859-1";

	/** The encoding to use (user-settable) */
	private String encoding = __DEFAULT_ENCODING;

	private StringBuffer __commandBuffer;

	BufferedReader _reader;
	BufferedWriter _writer;
	int _replyCode;
	ArrayList<String> _replyLines;
	boolean _newReplyString;
	String _replyString;

	/***
	 * A ProtocolCommandSupport object used to manage the registering of
	 * ProtocolCommandListeners and te firing of ProtocolCommandEvents.
	 ***/
	protected ProtocolCommandSupport _commandSupport_;

	/***
	 * The default SMTP constructor. Sets the default port to
	 * <code>DEFAULT_PORT</code> and initializes internal data structures
	 * for saving SMTP reply information.
	 ***/
	public SMTP() {
		setDefaultPort(DEFAULT_PORT);
		__commandBuffer = new StringBuffer();
		_replyLines = new ArrayList<String>();
		_newReplyString = false;
		_replyString = null;
		_commandSupport_ = new ProtocolCommandSupport(this);
	}

	/**
	 * Overloaded constructor where the user may specify a default encoding.
	 * 
	 * @param encoding
	 * @since 2.0
	 */
	public SMTP(String encoding) {
		this();
		this.encoding = encoding;
	}

	private int __sendCommand(String command, String args, boolean includeSpace) throws IOException {
		String message;

		__commandBuffer.setLength(0);
		__commandBuffer.append(command);

		if(args != null){
			if(includeSpace){
				__commandBuffer.append(' ');
			}
			__commandBuffer.append(args);
		}

		__commandBuffer.append(SocketClient.NETASCII_EOL);

		_writer.write(message = __commandBuffer.toString());
		_writer.flush();

		if(_commandSupport_.getListenerCount() > 0){
			_commandSupport_.fireCommandSent(command, message);
		}

		__getReply();
		return _replyCode;
	}

	private int __sendCommand(int command, String args, boolean includeSpace) throws IOException {
		return __sendCommand(SMTPCommand.getCommand(command), args, includeSpace);
	}

	private void __getReply() throws IOException {
		int length;

		_newReplyString = true;
		_replyLines.clear();

		String line = _reader.readLine();

		if(line == null){
			throw new SMTPConnectionClosedException("Connection closed without indication.");
		}

		// In case we run into an anomaly we don't want fatal index exceptions
		// to be thrown.
		length = line.length();
		if(length < 3){
			throw new MalformedServerReplyException("Truncated server reply: " + line);
		}

		try{
			String code = line.substring(0, 3);
			_replyCode = Integer.parseInt(code);
		} catch(NumberFormatException e){
			throw new MalformedServerReplyException("Could not parse response code.\nServer Reply: " + line);
		}

		_replyLines.add(line);

		// Get extra lines if message continues.
		if((length > 3) && (line.charAt(3) == '-')){
			do{
				line = _reader.readLine();

				if(line == null){
					throw new SMTPConnectionClosedException("Connection closed without indication.");
				}

				_replyLines.add(line);

				// The length() check handles problems that could arise from
				// readLine()
				// returning too soon after encountering a naked CR or some
				// other
				// anomaly.
			} while(!((line.length() >= 4) && (line.charAt(3) != '-') && Character.isDigit(line.charAt(0))));
			// This is too strong a condition because a non-conforming server
			// could screw things up like ftp.funet.fi does for FTP
			// line.startsWith(code)));
		}

		if(_commandSupport_.getListenerCount() > 0){
			_commandSupport_.fireReplyReceived(_replyCode, getReplyString());
		}

		if(_replyCode == SMTPReply.SERVICE_NOT_AVAILABLE){
			throw new SMTPConnectionClosedException(
					"SMTP response 421 received.  Server closed connection.");
		}
	}

	/*** Initiates control connections and gets initial reply. ***/
	@Override
	protected void _connectAction_() throws IOException {
		super._connectAction_();
		_reader = new BufferedReader(new InputStreamReader(_input_, encoding));
		_writer = new BufferedWriter(new OutputStreamWriter(_output_, encoding));
		__getReply();

	}

	/***
	 * Adds a ProtocolCommandListener. Delegates this task to
	 * {@link #_commandSupport_ _commandSupport_ }.
	 * <p>
	 * 
	 * @param listener
	 *            The ProtocolCommandListener to add.
	 ***/
	public void addProtocolCommandListener(ProtocolCommandListener listener) {
		_commandSupport_.addProtocolCommandListener(listener);
	}

	/***
	 * Removes a ProtocolCommandListener. Delegates this task to
	 * {@link #_commandSupport_ _commandSupport_ }.
	 * <p>
	 * 
	 * @param listener
	 *            The ProtocolCommandListener to remove.
	 ***/
	public void removeProtocolCommandistener(ProtocolCommandListener listener) {
		_commandSupport_.removeProtocolCommandListener(listener);
	}

	/***
	 * Closes the connection to the SMTP server and sets to null
	 * some internal data so that the memory may be reclaimed by the
	 * garbage collector. The reply text and code information from the
	 * last command is voided so that the memory it used may be reclaimed.
	 * <p>
	 * 
	 * @exception IOException
	 *                If an error occurs while disconnecting.
	 ***/
	@Override
	public void disconnect() throws IOException {
		super.disconnect();
		_reader = null;
		_writer = null;
		_replyString = null;
		_replyLines.clear();
		_newReplyString = false;
	}

	/***
	 * Sends an SMTP command to the server, waits for a reply and returns the
	 * numerical response code. After invocation, for more detailed
	 * information, the actual reply text can be accessed by calling
	 * {@link #getReplyString getReplyString } or {@link #getReplyStrings
	 * getReplyStrings }.
	 * <p>
	 * 
	 * @param command
	 *            The text representation of the SMTP command to send.
	 * @param args
	 *            The arguments to the SMTP command. If this parameter is
	 *            set to null, then the command is sent with no argument.
	 * @return The integer value of the SMTP reply code returned by the server
	 *         in response to the command.
	 * @exception SMTPConnectionClosedException
	 *                If the SMTP server prematurely closes the connection as a
	 *                result
	 *                of the client being idle or some other reason causing the
	 *                server
	 *                to send SMTP reply code 421. This exception may be caught
	 *                either
	 *                as an IOException or independently as itself.
	 * @exception IOException
	 *                If an I/O error occurs while either sending the
	 *                command or receiving the server reply.
	 ***/
	public int sendCommand(String command, String args) throws IOException {
		return __sendCommand(command, args, true);
	}

	/***
	 * Sends an SMTP command to the server, waits for a reply and returns the
	 * numerical response code. After invocation, for more detailed
	 * information, the actual reply text can be accessed by calling
	 * {@link #getReplyString getReplyString } or {@link #getReplyStrings
	 * getReplyStrings }.
	 * <p>
	 * 
	 * @param command
	 *            The SMTPCommand constant corresponding to the SMTP command
	 *            to send.
	 * @param args
	 *            The arguments to the SMTP command. If this parameter is
	 *            set to null, then the command is sent with no argument.
	 * @return The integer value of the SMTP reply code returned by the server
	 *         in response to the command.
	 * @exception SMTPConnectionClosedException
	 *                If the SMTP server prematurely closes the connection as a
	 *                result
	 *                of the client being idle or some other reason causing the
	 *                server
	 *                to send SMTP reply code 421. This exception may be caught
	 *                either
	 *                as an IOException or independently as itself.
	 * @exception IOException
	 *                If an I/O error occurs while either sending the
	 *                command or receiving the server reply.
	 ***/
	public int sendCommand(int command, String args) throws IOException {
		return sendCommand(SMTPCommand.getCommand(command), args);
	}

	/***
	 * Sends an SMTP command with no arguments to the server, waits for a
	 * reply and returns the numerical response code. After invocation, for
	 * more detailed information, the actual reply text can be accessed by
	 * calling {@link #getReplyString getReplyString } or
	 * {@link #getReplyStrings getReplyStrings }.
	 * <p>
	 * 
	 * @param command
	 *            The text representation of the SMTP command to send.
	 * @return The integer value of the SMTP reply code returned by the server
	 *         in response to the command.
	 * @exception SMTPConnectionClosedException
	 *                If the SMTP server prematurely closes the connection as a
	 *                result
	 *                of the client being idle or some other reason causing the
	 *                server
	 *                to send SMTP reply code 421. This exception may be caught
	 *                either
	 *                as an IOException or independently as itself.
	 * @exception IOException
	 *                If an I/O error occurs while either sending the
	 *                command or receiving the server reply.
	 ***/
	public int sendCommand(String command) throws IOException {
		return sendCommand(command, null);
	}

	/***
	 * Sends an SMTP command with no arguments to the server, waits for a
	 * reply and returns the numerical response code. After invocation, for
	 * more detailed information, the actual reply text can be accessed by
	 * calling {@link #getReplyString getReplyString } or
	 * {@link #getReplyStrings getReplyStrings }.
	 * <p>
	 * 
	 * @param command
	 *            The SMTPCommand constant corresponding to the SMTP command
	 *            to send.
	 * @return The integer value of the SMTP reply code returned by the server
	 *         in response to the command.
	 * @exception SMTPConnectionClosedException
	 *                If the SMTP server prematurely closes the connection as a
	 *                result
	 *                of the client being idle or some other reason causing the
	 *                server
	 *                to send SMTP reply code 421. This exception may be caught
	 *                either
	 *                as an IOException or independently as itself.
	 * @exception IOException
	 *                If an I/O error occurs while either sending the
	 *                command or receiving the server reply.
	 ***/
	public int sendCommand(int command) throws IOException {
		return sendCommand(command, null);
	}

	/***
	 * Returns the integer value of the reply code of the last SMTP reply.
	 * You will usually only use this method after you connect to the
	 * SMTP server to check that the connection was successful since
	 * <code> connect </code> is of type void.
	 * <p>
	 * 
	 * @return The integer value of the reply code of the last SMTP reply.
	 ***/
	public int getReplyCode() {
		return _replyCode;
	}

	/***
	 * Fetches a reply from the SMTP server and returns the integer reply
	 * code. After calling this method, the actual reply text can be accessed
	 * from either calling {@link #getReplyString getReplyString } or
	 * {@link #getReplyStrings getReplyStrings }. Only use this
	 * method if you are implementing your own SMTP client or if you need to
	 * fetch a secondary response from the SMTP server.
	 * <p>
	 * 
	 * @return The integer value of the reply code of the fetched SMTP reply.
	 * @exception SMTPConnectionClosedException
	 *                If the SMTP server prematurely closes the connection as a
	 *                result
	 *                of the client being idle or some other reason causing the
	 *                server
	 *                to send SMTP reply code 421. This exception may be caught
	 *                either
	 *                as an IOException or independently as itself.
	 * @exception IOException
	 *                If an I/O error occurs while receiving the
	 *                server reply.
	 ***/
	public int getReply() throws IOException {
		__getReply();
		return _replyCode;
	}

	/***
	 * Returns the lines of text from the last SMTP server response as an array
	 * of strings, one entry per line. The end of line markers of each are
	 * stripped from each line.
	 * <p>
	 * 
	 * @return The lines of text from the last SMTP response as an array.
	 ***/
	public String[] getReplyStrings() {
		return _replyLines.toArray(new String[_replyLines.size()]);
	}

	/***
	 * Returns the entire text of the last SMTP server response exactly
	 * as it was received, including all end of line markers in NETASCII
	 * format.
	 * <p>
	 * 
	 * @return The entire text from the last SMTP response as a String.
	 ***/
	public String getReplyString() {
		StringBuilder buffer;

		if(!_newReplyString){
			return _replyString;
		}

		buffer = new StringBuilder();

		for(String line : _replyLines){
			buffer.append(line);
			buffer.append(SocketClient.NETASCII_EOL);
		}

		_newReplyString = false;

		return (_replyString = buffer.toString());
	}

	/***
	 * A convenience method to send the SMTP HELO command to the server,
	 * receive the reply, and return the reply code.
	 * <p>
	 * 
	 * @param hostname
	 *            The hostname of the sender.
	 * @return The reply code received from the server.
	 * @exception SMTPConnectionClosedException
	 *                If the SMTP server prematurely closes the connection as a
	 *                result
	 *                of the client being idle or some other reason causing the
	 *                server
	 *                to send SMTP reply code 421. This exception may be caught
	 *                either
	 *                as an IOException or independently as itself.
	 * @exception IOException
	 *                If an I/O error occurs while either sending the
	 *                command or receiving the server reply.
	 ***/
	public int helo(String hostname) throws IOException {
		return sendCommand(SMTPCommand.HELO, hostname);
	}

	/***
	 * A convenience method to send the SMTP MAIL command to the server,
	 * receive the reply, and return the reply code.
	 * <p>
	 * 
	 * @param reversePath
	 *            The reverese path.
	 * @return The reply code received from the server.
	 * @exception SMTPConnectionClosedException
	 *                If the SMTP server prematurely closes the connection as a
	 *                result
	 *                of the client being idle or some other reason causing the
	 *                server
	 *                to send SMTP reply code 421. This exception may be caught
	 *                either
	 *                as an IOException or independently as itself.
	 * @exception IOException
	 *                If an I/O error occurs while either sending the
	 *                command or receiving the server reply.
	 ***/
	public int mail(String reversePath) throws IOException {
		return __sendCommand(SMTPCommand.MAIL, reversePath, false);
	}

	/***
	 * A convenience method to send the SMTP RCPT command to the server,
	 * receive the reply, and return the reply code.
	 * <p>
	 * 
	 * @param forwardPath
	 *            The forward path.
	 * @return The reply code received from the server.
	 * @exception SMTPConnectionClosedException
	 *                If the SMTP server prematurely closes the connection as a
	 *                result
	 *                of the client being idle or some other reason causing the
	 *                server
	 *                to send SMTP reply code 421. This exception may be caught
	 *                either
	 *                as an IOException or independently as itself.
	 * @exception IOException
	 *                If an I/O error occurs while either sending the
	 *                command or receiving the server reply.
	 ***/
	public int rcpt(String forwardPath) throws IOException {
		return __sendCommand(SMTPCommand.RCPT, forwardPath, false);
	}

	/***
	 * A convenience method to send the SMTP DATA command to the server,
	 * receive the reply, and return the reply code.
	 * <p>
	 * 
	 * @return The reply code received from the server.
	 * @exception SMTPConnectionClosedException
	 *                If the SMTP server prematurely closes the connection as a
	 *                result
	 *                of the client being idle or some other reason causing the
	 *                server
	 *                to send SMTP reply code 421. This exception may be caught
	 *                either
	 *                as an IOException or independently as itself.
	 * @exception IOException
	 *                If an I/O error occurs while either sending the
	 *                command or receiving the server reply.
	 ***/
	public int data() throws IOException {
		return sendCommand(SMTPCommand.DATA);
	}

	/***
	 * A convenience method to send the SMTP SEND command to the server,
	 * receive the reply, and return the reply code.
	 * <p>
	 * 
	 * @param reversePath
	 *            The reverese path.
	 * @return The reply code received from the server.
	 * @exception SMTPConnectionClosedException
	 *                If the SMTP server prematurely closes the connection as a
	 *                result
	 *                of the client being idle or some other reason causing the
	 *                server
	 *                to send SMTP reply code 421. This exception may be caught
	 *                either
	 *                as an IOException or independently as itself.
	 * @exception IOException
	 *                If an I/O error occurs while either sending the
	 *                command or receiving the server reply.
	 ***/
	public int send(String reversePath) throws IOException {
		return sendCommand(SMTPCommand.SEND, reversePath);
	}

	/***
	 * A convenience method to send the SMTP SOML command to the server,
	 * receive the reply, and return the reply code.
	 * <p>
	 * 
	 * @param reversePath
	 *            The reverese path.
	 * @return The reply code received from the server.
	 * @exception SMTPConnectionClosedException
	 *                If the SMTP server prematurely closes the connection as a
	 *                result
	 *                of the client being idle or some other reason causing the
	 *                server
	 *                to send SMTP reply code 421. This exception may be caught
	 *                either
	 *                as an IOException or independently as itself.
	 * @exception IOException
	 *                If an I/O error occurs while either sending the
	 *                command or receiving the server reply.
	 ***/
	public int soml(String reversePath) throws IOException {
		return sendCommand(SMTPCommand.SOML, reversePath);
	}

	/***
	 * A convenience method to send the SMTP SAML command to the server,
	 * receive the reply, and return the reply code.
	 * <p>
	 * 
	 * @param reversePath
	 *            The reverese path.
	 * @return The reply code received from the server.
	 * @exception SMTPConnectionClosedException
	 *                If the SMTP server prematurely closes the connection as a
	 *                result
	 *                of the client being idle or some other reason causing the
	 *                server
	 *                to send SMTP reply code 421. This exception may be caught
	 *                either
	 *                as an IOException or independently as itself.
	 * @exception IOException
	 *                If an I/O error occurs while either sending the
	 *                command or receiving the server reply.
	 ***/
	public int saml(String reversePath) throws IOException {
		return sendCommand(SMTPCommand.SAML, reversePath);
	}

	/***
	 * A convenience method to send the SMTP RSET command to the server,
	 * receive the reply, and return the reply code.
	 * <p>
	 * 
	 * @return The reply code received from the server.
	 * @exception SMTPConnectionClosedException
	 *                If the SMTP server prematurely closes the connection as a
	 *                result
	 *                of the client being idle or some other reason causing the
	 *                server
	 *                to send SMTP reply code 421. This exception may be caught
	 *                either
	 *                as an IOException or independently as itself.
	 * @exception IOException
	 *                If an I/O error occurs while either sending the
	 *                command or receiving the server reply.
	 ***/
	public int rset() throws IOException {
		return sendCommand(SMTPCommand.RSET);
	}

	/***
	 * A convenience method to send the SMTP VRFY command to the server,
	 * receive the reply, and return the reply code.
	 * <p>
	 * 
	 * @param user
	 *            The user address to verify.
	 * @return The reply code received from the server.
	 * @exception SMTPConnectionClosedException
	 *                If the SMTP server prematurely closes the connection as a
	 *                result
	 *                of the client being idle or some other reason causing the
	 *                server
	 *                to send SMTP reply code 421. This exception may be caught
	 *                either
	 *                as an IOException or independently as itself.
	 * @exception IOException
	 *                If an I/O error occurs while either sending the
	 *                command or receiving the server reply.
	 ***/
	public int vrfy(String user) throws IOException {
		return sendCommand(SMTPCommand.VRFY, user);
	}

	/***
	 * A convenience method to send the SMTP VRFY command to the server,
	 * receive the reply, and return the reply code.
	 * <p>
	 * 
	 * @param name
	 *            The name to expand.
	 * @return The reply code received from the server.
	 * @exception SMTPConnectionClosedException
	 *                If the SMTP server prematurely closes the connection as a
	 *                result
	 *                of the client being idle or some other reason causing the
	 *                server
	 *                to send SMTP reply code 421. This exception may be caught
	 *                either
	 *                as an IOException or independently as itself.
	 * @exception IOException
	 *                If an I/O error occurs while either sending the
	 *                command or receiving the server reply.
	 ***/
	public int expn(String name) throws IOException {
		return sendCommand(SMTPCommand.EXPN, name);
	}

	/***
	 * A convenience method to send the SMTP HELP command to the server,
	 * receive the reply, and return the reply code.
	 * <p>
	 * 
	 * @return The reply code received from the server.
	 * @exception SMTPConnectionClosedException
	 *                If the SMTP server prematurely closes the connection as a
	 *                result
	 *                of the client being idle or some other reason causing the
	 *                server
	 *                to send SMTP reply code 421. This exception may be caught
	 *                either
	 *                as an IOException or independently as itself.
	 * @exception IOException
	 *                If an I/O error occurs while either sending the
	 *                command or receiving the server reply.
	 ***/
	public int help() throws IOException {
		return sendCommand(SMTPCommand.HELP);
	}

	/***
	 * A convenience method to send the SMTP HELP command to the server,
	 * receive the reply, and return the reply code.
	 * <p>
	 * 
	 * @param command
	 *            The command name on which to request help.
	 * @return The reply code received from the server.
	 * @exception SMTPConnectionClosedException
	 *                If the SMTP server prematurely closes the connection as a
	 *                result
	 *                of the client being idle or some other reason causing the
	 *                server
	 *                to send SMTP reply code 421. This exception may be caught
	 *                either
	 *                as an IOException or independently as itself.
	 * @exception IOException
	 *                If an I/O error occurs while either sending the
	 *                command or receiving the server reply.
	 ***/
	public int help(String command) throws IOException {
		return sendCommand(SMTPCommand.HELP, command);
	}

	/***
	 * A convenience method to send the SMTP NOOP command to the server,
	 * receive the reply, and return the reply code.
	 * <p>
	 * 
	 * @return The reply code received from the server.
	 * @exception SMTPConnectionClosedException
	 *                If the SMTP server prematurely closes the connection as a
	 *                result
	 *                of the client being idle or some other reason causing the
	 *                server
	 *                to send SMTP reply code 421. This exception may be caught
	 *                either
	 *                as an IOException or independently as itself.
	 * @exception IOException
	 *                If an I/O error occurs while either sending the
	 *                command or receiving the server reply.
	 ***/
	public int noop() throws IOException {
		return sendCommand(SMTPCommand.NOOP);
	}

	/***
	 * A convenience method to send the SMTP TURN command to the server,
	 * receive the reply, and return the reply code.
	 * <p>
	 * 
	 * @return The reply code received from the server.
	 * @exception SMTPConnectionClosedException
	 *                If the SMTP server prematurely closes the connection as a
	 *                result
	 *                of the client being idle or some other reason causing the
	 *                server
	 *                to send SMTP reply code 421. This exception may be caught
	 *                either
	 *                as an IOException or independently as itself.
	 * @exception IOException
	 *                If an I/O error occurs while either sending the
	 *                command or receiving the server reply.
	 ***/
	public int turn() throws IOException {
		return sendCommand(SMTPCommand.TURN);
	}

	/***
	 * A convenience method to send the SMTP QUIT command to the server,
	 * receive the reply, and return the reply code.
	 * <p>
	 * 
	 * @return The reply code received from the server.
	 * @exception SMTPConnectionClosedException
	 *                If the SMTP server prematurely closes the connection as a
	 *                result
	 *                of the client being idle or some other reason causing the
	 *                server
	 *                to send SMTP reply code 421. This exception may be caught
	 *                either
	 *                as an IOException or independently as itself.
	 * @exception IOException
	 *                If an I/O error occurs while either sending the
	 *                command or receiving the server reply.
	 ***/
	public int quit() throws IOException {
		return sendCommand(SMTPCommand.QUIT);
	}

}

/*
 * Emacs configuration
 * Local variables: **
 * mode: java **
 * c-basic-offset: 4 **
 * indent-tabs-mode: nil **
 * End: **
 */
