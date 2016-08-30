package com.inted.as.telephony;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.sdp.SdpException;
import javax.sdp.SdpFactory;
import javax.sdp.SessionDescription;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.sip.B2buaHelper;
import javax.servlet.sip.Proxy;
import javax.servlet.sip.SipErrorEvent;
import javax.servlet.sip.SipErrorListener;
import javax.servlet.sip.SipFactory;
import javax.servlet.sip.SipServlet;
import javax.servlet.sip.SipServletMessage;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipSession;

/**
 * Servlet implementation class Main
 */
public class Main extends SipServlet implements SipErrorListener {

	private static final long serialVersionUID = 1L;

	private CsdrClient client;
	private SipFactory sipFactory;
	private List<String> activeCalls;

	/**
	 * @see SipServlet#SipServlet()
	 */
	public Main() {
		client = new CsdrClient();
		activeCalls = new ArrayList<String>();
	}

	@Override
	public void init(ServletConfig servletConfig) throws ServletException {
		System.out.println("The Telephony-FE has been started");
		super.init(servletConfig);
		sipFactory = (SipFactory) getServletContext().getAttribute(SIP_FACTORY);
	}

	@Override
	protected void doInvite(SipServletRequest request) throws ServletException, IOException {
		System.out.println("INVITE-REQUEST received");
		if (request.isInitial()) {
			Proxy proxy = request.getProxy();
			proxy.proxyTo(request.getRequestURI());
		}
	}

	@Override
	protected void doAck(SipServletRequest request) throws IOException, ServletException {
		System.out.println("ACK-REQUEST received");

		if (request.getSession().isValid())
			super.doAck(request);

	}

	@Override
	protected void doBye(SipServletRequest request) throws ServletException, IOException {
		System.out.println("BYE-REQUEST received");

		if (request.getSession().isValid())
			request.createResponse(SipServletResponse.SC_OK).send();

	}

	@Override
	protected void doCancel(SipServletRequest request) throws ServletException, IOException {
		System.out.println("CANCEL-REQUEST received");

		if (request.getSession().isValid())
			super.doCancel(request);

	}

	@Override
	protected void doSuccessResponse(SipServletResponse sipServletResponse) throws ServletException, IOException {
		System.out.println("SUCCESS-RESPONSE received");

		if (sipServletResponse.getSession().isValid()) {

			super.doSuccessResponse(sipServletResponse);

			String cseq = sipServletResponse.getHeader("CSeq");
			String callId = sipServletResponse.getCallId();

			if (cseq != null && callId != null && cseq.contains("INVITE") && !activeCalls.contains(callId)) {

				activeCalls.add(callId);
				CallDetailRecord cdr = createCdr(sipServletResponse);
				client.addCallDetailRecord(cdr);

				try {
					SessionData sd = createData(sipServletResponse);
					client.addSessionData(sd);
				} catch (SdpException e) {
					e.printStackTrace();
				}

			} 

		}
	}

	@Override
	protected void doErrorResponse(SipServletResponse sipServletResponse) throws ServletException, IOException {
		System.out.println("ERROR-RESPONSE received");
		super.doErrorResponse(sipServletResponse);
	}

	@Override
	protected void doProvisionalResponse(SipServletResponse sipServletResponse) throws ServletException, IOException {
		System.out.println("PROVISIONAL-RESPONSE received");
		super.doProvisionalResponse(sipServletResponse);
	}

	@Override
	protected void doInfo(SipServletRequest request) throws ServletException, IOException {
		System.out.println("INFO-REQUEST received");
		super.doInfo(request);
	}

	@Override
	protected void doUpdate(SipServletRequest request) throws ServletException, IOException {
		System.out.println("UPDATE-REQUEST received");
		super.doUpdate(request);
	}

	@Override
	public void noAckReceived(SipErrorEvent arg0) {
		System.out.println(" Error: noAckReceived.");
	}

	@Override
	public void noPrackReceived(SipErrorEvent arg0) {
		System.out.println(" Error: noPrackReceived.");

	}

	public CallDetailRecord createCdr(SipServletMessage msg) {

		CallDetailRecord cdr = new CallDetailRecord();
		SipSession session = msg.getSession();

		cdr.setCallId(msg.getCallId());
		cdr.setContact(msg.getHeader("Contact"));
		cdr.setContainsSdp(msg.getContentLength() > 0);
		cdr.setcSeq(msg.getHeader("CSeq"));

		long duration = session.getLastAccessedTime() - session.getCreationTime();
		cdr.setDuration("" + duration);

		cdr.setFrom(session.getRemoteParty().getValue());
		cdr.setTo(session.getLocalParty().getValue());
		cdr.setSipMethod(msg.getMethod());

		if(msg.getHeader("User-Agent") != null)
		cdr.setUserAgent(msg.getHeader("User-Agent"));
		
		cdr.setSessionExpires(msg.getExpires());

		return cdr;
	}

	public SessionData createData(SipServletMessage msg) throws IOException, SdpException {
		SessionData sd = new SessionData();
		SipSession session = msg.getSession();
		sd.setLastKnownSessionId(session.getId());
		sd.setLastKnownState(session.getState().name());

		if (msg.getContentLength() > 0) {

			String content = new String(msg.getRawContent());
			sd = addSdp(sd, content);
		}

		sd.setSubscriberUri(msg.getFrom().getURI().toString());

		if (msg.getHeader("User-Agent") != null)
			sd.setUserAgent(msg.getHeader("User-Agent"));

		return sd;
	}

	public SessionData addSdp(SessionData sd, String content) throws SdpException {

		SdpFactory sdpFactory = SdpFactory.getInstance();
		SessionDescription sdc = sdpFactory.createSessionDescription(content);
		Vector mds = sdc.getMediaDescriptions(true);

		Iterator mdIt = mds.iterator();
		while (mdIt.hasNext()) {

			String next = mdIt.next().toString();
			String[] comp = next.split(" ");

			if (comp.length > 2) {
				String type = comp[0].substring(2, comp[0].length());
				String port = comp[1];
				String protocol = comp[2];

				sd.setMediaPort(port);
				sd.setMediaProtocol(protocol);
				sd.setMediaType(type);
			}

		}
		return sd;
	}

}
