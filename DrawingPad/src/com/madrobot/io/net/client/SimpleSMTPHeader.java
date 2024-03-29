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

/***
 * This class is used to construct a bare minimum
 * acceptable header for an email message. To construct more
 * complicated headers you should refer to RFC 822. When the
 * Java Mail API is finalized, you will be
 * able to use it to compose fully compliant Internet text messages.
 * <p>
 * The main purpose of the class is to faciliatate the mail sending process, by
 * relieving the programmer from having to explicitly format a simple message
 * header. For example:
 * 
 * <pre>
 * writer = client.sendMessageData();
 * if(writer == null) // failure
 *   return false;
 * header =
 *    new SimpleSMTPHeader("foobar@foo.com", "foo@bar.com" "Just testing");
 * header.addCC("bar@foo.com");
 * header.addHeaderField("Organization", "Foobar, Inc.");
 * writer.write(header.toString());
 * writer.write("This is just a test");
 * writer.close();
 * if(!client.completePendingCommand()) // failure
 *   return false;
 * </pre>
 * <p>
 * <p>
 * 
 * @see SMTPClient
 ***/

public class SimpleSMTPHeader {
	private String __subject, __from, __to;
	private StringBuffer __headerFields, __cc;

	/***
	 * Creates a new SimpleSMTPHeader instance initialized with the given
	 * from, to, and subject header field values.
	 * <p>
	 * 
	 * @param from
	 *            The value of the <code>From:</code> header field. This
	 *            should be the sender's email address.
	 * @param to
	 *            The value of the <code>To:</code> header field. This
	 *            should be the recipient's email address.
	 * @param subject
	 *            The value of the <code>Subject:</code> header field.
	 *            This should be the subject of the message.
	 ***/
	public SimpleSMTPHeader(String from, String to, String subject) {
		__to = to;
		__from = from;
		__subject = subject;
		__headerFields = new StringBuffer();
		__cc = null;
	}

	/***
	 * Adds an arbitrary header field with the given value to the article
	 * header. These headers will be written before the From, To, Subject, and
	 * Cc fields when the SimpleSMTPHeader is convertered to a string.
	 * An example use would be:
	 * 
	 * <pre>
	 * header.addHeaderField(&quot;Organization&quot;, &quot;Foobar, Inc.&quot;);
	 * </pre>
	 * <p>
	 * 
	 * @param headerField
	 *            The header field to add, not including the colon.
	 * @param value
	 *            The value of the added header field.
	 ***/
	public void addHeaderField(String headerField, String value) {
		__headerFields.append(headerField);
		__headerFields.append(": ");
		__headerFields.append(value);
		__headerFields.append('\n');
	}

	/***
	 * Add an email address to the CC (carbon copy or courtesy copy) list.
	 * <p>
	 * 
	 * @param address
	 *            The email address to add to the CC list.
	 ***/
	public void addCC(String address) {
		if(__cc == null){
			__cc = new StringBuffer();
		} else{
			__cc.append(", ");
		}

		__cc.append(address);
	}

	/***
	 * Converts the SimpleSMTPHeader to a properly formatted header in
	 * the form of a String, including the blank line used to separate
	 * the header from the article body. The header fields CC and Subject
	 * are only included when they are non-null.
	 * <p>
	 * 
	 * @return The message header in the form of a String.
	 ***/
	@Override
	public String toString() {
		StringBuilder header = new StringBuilder();

		if(__headerFields.length() > 0){
			header.append(__headerFields.toString());
		}

		header.append("From: ");
		header.append(__from);
		header.append("\nTo: ");
		header.append(__to);

		if(__cc != null){
			header.append("\nCc: ");
			header.append(__cc.toString());
		}

		if(__subject != null){
			header.append("\nSubject: ");
			header.append(__subject);
		}

		header.append('\n');
		header.append('\n');

		return header.toString();
	}
}
