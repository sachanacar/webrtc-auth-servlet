package com.voxbone.webrtc;

import com.voxbone.webrtc.encoding.HmacSha1Encoder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.SignatureException;

/**
 * This class purpose is to return a string representing a javascript variable declaration.
 * The javascript variable will contains everything needed to perform an authentication
 * toward voxbone webrtc ephemeral auth server.
 *
 * @author Vincent Morsiani <vmorsiani@voxbone.com>
 * @since 2014-03-07
 */
public class WebrtcAuthServlet extends HttpServlet{
	public static final String PARAM_USERNAME = "com.voxbone.webrtc.auth.username";
	public static final String PARAM_KEY = "com.voxbone.webrtc.auth.secret";
	public static final String PARAM_TTL = "com.voxbone.webrtc.auth.TTL";
	public static final String PARAM_VAR_NAME = "com.voxbone.webrtc.auth.varname";


	private String username;
	private String varName = "voxrtc_config";
	private String secret;
	private long ttl = 300;

	@Override
	public void init() throws ServletException {
		this.username = super.getInitParameter(PARAM_USERNAME);
		if(username == null){
			throw new ServletException(PARAM_USERNAME+" has not been set");
		}
		this.secret = super.getInitParameter(PARAM_KEY);
		if(secret == null){
			throw new ServletException(PARAM_KEY+" has not been set");
		}

		if(super.getInitParameter( PARAM_TTL) !=null){
			this.ttl = Long.parseLong(super.getInitParameter(PARAM_TTL));
		}

		if(super.getInitParameter( PARAM_VAR_NAME) != null){
			this.varName = super.getInitParameter( PARAM_VAR_NAME );
		}

		super.init();
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		this.service(req,resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		this.service(req,resp);
	}

	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		long time = this.getTime();
		long expires = time + ttl;

		String key;
		try {
			key = this.createKey(username,secret,expires);
		} catch (SignatureException e) {
			throw new ServletException("unable to compute key",e);
		}
		String body = String.format("var %s = {\"key\":\"%s\",\"expires\":%s,\"username\":\"%s\"};\n",varName,key,expires,username);
		resp.setContentType("text/javascript");
		resp.getWriter().write(body);
	}

	private String createKey(String username, String secret, long expires) throws SignatureException {
		String hash = HmacSha1Encoder.encode(expires + ":" + username, secret);
		return clearHmac(hash);
	}

	private String clearHmac(String hmac){
		//Ensure the size of the returned hash
		while((hmac.length() % 4 != 0)){
			hmac += "=";
		}

		hmac = hmac.replaceAll(" ","+");
		return hmac;
	}

	private long getTime(){
		return System.currentTimeMillis()/1000;
	}
}
