/*
* This file is part of the INTO-CPS toolchain.
*
* Copyright (c) 2017-CurrentYear, INTO-CPS Association,
* c/o Professor Peter Gorm Larsen, Department of Engineering
* Finlandsgade 22, 8200 Aarhus N.
*
* All rights reserved.
*
* THIS PROGRAM IS PROVIDED UNDER THE TERMS OF GPL VERSION 3 LICENSE OR
* THIS INTO-CPS ASSOCIATION PUBLIC LICENSE VERSION 1.0.
* ANY USE, REPRODUCTION OR DISTRIBUTION OF THIS PROGRAM CONSTITUTES
* RECIPIENT'S ACCEPTANCE OF THE OSMC PUBLIC LICENSE OR THE GPL 
* VERSION 3, ACCORDING TO RECIPIENTS CHOICE.
*
* The INTO-CPS toolchain  and the INTO-CPS Association Public License 
* are obtained from the INTO-CPS Association, either from the above address,
* from the URLs: http://www.into-cps.org, and in the INTO-CPS toolchain distribution.
* GNU version 3 is obtained from: http://www.gnu.org/copyleft/gpl.html.
*
* This program is distributed WITHOUT ANY WARRANTY; without
* even the implied warranty of  MERCHANTABILITY or FITNESS FOR
* A PARTICULAR PURPOSE, EXCEPT AS EXPRESSLY SET FORTH IN THE
* BY RECIPIENT SELECTED SUBSIDIARY LICENSE CONDITIONS OF
* THE INTO-CPS ASSOCIATION.
*
* See the full INTO-CPS Association Public License conditions for more details.
*/

/*
* Author:
*		Kenneth Lausdahl
*		Casper Thule
*/
package org.intocps.orchestration.coe.httpserver;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoWSD;

/**
 * Created by ctha on 29-03-2016.
 */
public class RequestHandler
{

	final static Logger logger = LoggerFactory.getLogger(NanoWebSocketImpl.class);
	private final SessionController sessionController;
	final ObjectMapper mapper = new ObjectMapper();
	private final RequestProcessors requestProcessors;
	private Map<String, NanoWebSocketImpl> temporarySockets = new HashMap<>();

	public RequestHandler(SessionController sessionController,
			RequestProcessors requestProcessors)
	{
		this.requestProcessors = requestProcessors;
		this.sessionController = sessionController;
	}

	private static final String SIMULATE = "/simulate";
	private static final String SIMULATEASYNC = "/simulateasync";
	private static final String STOPSIMULATION = "/stopsimulation";
	private static final String INITIALIZE = "/initialize";
	private static final String RESULT = "/result";
	private static final String CREATESESSION = "/createSession";
	private static final String STATUS = "/status";
	private static final String DESTROY = "/destroy";
	private static final String UPLOAD = "/upload";
	private static final String WEBSOCKET = "/attachSession";
	private static final String VERSION = "/version";

	Map<String, IPageLoader> pages = new HashMap<String, IPageLoader>()
	{
		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		{
			put("/upload.html", createPageLoader(Response.MIME_HTML, "/upload.html"));
			put("/", createPageLoader(Response.MIME_HTML, "/coe-welcome.html"));
			put("/api/pdf", createPageLoader("application/pdf", "/coe-protocol/coe-protocol.pdf"));
			put("/api", createPageLoader(Response.MIME_HTML, "/coe-protocol/coe-protocol.html"));
			put("/api/", createPageLoader(Response.MIME_HTML, "/coe-protocol/coe-protocol.html"));
		}
	};

	/**
	 * This function constructs a default missing session error response
	 *
	 * @return a new response containing an error with missing session
	 */
	private static NanoHTTPD.Response getMissingSessionResponse()
	{
		return ProcessingUtils.newFixedLengthPlainResponse(NanoHTTPD.Response.Status.BAD_REQUEST, "Missing session");
	}

	/**
	 * This function open a socket. If a session does not exist with the given sessionId, then the socket is placed in
	 * temporarySockets, where it will be cleared. If a session does exist with the given sessionId, then the socket is
	 * added to the Coe corresponding to the sessionId.
	 *
	 * @param handshake
	 * @return
	 */
	public NanoWebSocketImpl handleSocketHandshake(
			NanoHTTPD.IHTTPSession handshake)
	{
		String uri = handshake.getUri();
		if (uri.startsWith(WEBSOCKET))
		{
			String sessionId = ProcessingUtils.getSessionFromUrl(uri, WEBSOCKET);
			NanoWebSocketImpl socket = new NanoWebSocketImpl(handshake, this, sessionId);
			if (sessionController.containsSession(sessionId))
			{
				sessionController.addSocket(sessionId, socket);
			} else
			{
				temporarySockets.put(sessionId, socket);
			}
			return socket;
		} else
		{
			logger.warn("Attempt to attach to websocket with uri: " + uri);
			return null;
		}
	}

	public void handleSocketOpen(String sessionId)
	{
		if (temporarySockets.containsKey(sessionId))
		{
			try
			{
				temporarySockets.remove(sessionId).close(NanoWSD.WebSocketFrame.CloseCode.UnsupportedData, "The session was invalid, closing socket.", false);
			} catch (IOException e)
			{
				logger.warn("Failed to close socket: " + e);
			}
		}
	}

	public void handleSocketClose(String sessionId)
	{
		if (temporarySockets.containsKey(sessionId))
		{
			try
			{
				temporarySockets.remove(sessionId).close(NanoWSD.WebSocketFrame.CloseCode.UnsupportedData, "The session was invalid, closing socket.", false);
			} catch (IOException e)
			{
				logger.warn("Failed to close socket: " + e);
			}
		}
		if (this.sessionController != null
				&& this.sessionController.containsSocket(sessionId))
		{
			this.sessionController.removeSocket(sessionId);
		}
	}

	public interface IPageLoader
	{
		InputStream load();

		String getMimeType();
	}

	final static IPageLoader createPageLoader(final String mimeType,
			final String path)
	{
		return new IPageLoader()
		{

			@Override public InputStream load()
			{
				return RequestHandler.class.getResourceAsStream(path);
			}

			@Override public String getMimeType()
			{
				return mimeType;
			}
		};
	}

	public NanoHTTPD.Response handleRequest(NanoHTTPD.IHTTPSession session)
			throws IOException, NanoHTTPD.ResponseException
	{
		String uri = session.getUri();
		if (pages.containsKey(uri))
		{
			return pages(session);
		} else if (uri.startsWith(STATUS))
		{
			return status(uri);
		} else if (uri.startsWith(CREATESESSION))
		{
			return requestProcessors.processCreateSession();
		} else if (uri.startsWith(INITIALIZE))
		{
			return initialize(session, uri);
		} else if (uri.startsWith(UPLOAD))
		{
			return upload(session, uri);
		} else if (uri.startsWith(SIMULATEASYNC))
		{
			return simulate(session, uri, true);
		} else if (uri.startsWith(SIMULATE))
		{
			return simulate(session, uri, false);
		} else if (uri.startsWith(RESULT))
		{
			return result(uri);
		} else if (uri.startsWith(DESTROY))
		{
			return destroy(uri);
		} else if (uri.startsWith(VERSION))
		{
			return version(uri);
		} else if (uri.startsWith(STOPSIMULATION))
		{
			return stopSimulation(uri);
		} else
		{

			String url = uri;

			if (url.startsWith("/api/"))
			{
				url = url.substring(5);
			} else if (url.startsWith("/api"))
			{
				url = url.substring(4);
			}

			InputStream stream = RequestHandler.class.getResourceAsStream(url);
			if (stream == null)
			{
				url = "/coe-protocol/" + url;
				url = url.replace("//", "/");
				stream = RequestHandler.class.getResourceAsStream(url);
			}

			if (stream != null)
			{
				stream.close();

				IPageLoader page = createPageLoader(Response.MIME_HTML, url);
				try
				{
					return NanoHTTPD.newChunkedResponse(NanoHTTPD.Response.Status.OK, page.getMimeType(), page.load());
				} catch (Exception e)
				{
					logger.warn("Client tried to obtain URI: {} but failed obtained from: {}", uri, url);
				}
			} else
			{
				logger.warn("Unable to find: {} at: {}", uri, url);
			}
		}
		return ProcessingUtils.newFixedLengthPlainResponse(NanoHTTPD.Response.Status.NOT_IMPLEMENTED,
				"The uri " + session.getUri() + " is not supported.");
	}

	private NanoHTTPD.Response version(String uri) throws IOException
	{
		return requestProcessors.processVersion();
	}

	private NanoHTTPD.Response destroy(String uri) throws IOException
	{
		String sessionId = ProcessingUtils.getSessionFromUrl(uri, DESTROY);
		if (sessionId != null
				&& this.sessionController.containsSession(sessionId))
		{
			return requestProcessors.processDestroy(sessionId);
		}
		return getMissingSessionResponse();
	}

	private NanoHTTPD.Response result(String uri) throws IOException
	{
		// slash and at least one number char
		if (uri.length() > RESULT.length() + 1)
		{
			String[] args = uri.substring((RESULT + "/").length()).split("/");
			if (args.length > 0)
			{
				String sessionId = args[0];
				if (sessionId != null
						&& this.sessionController.containsSession(sessionId))
				{
					boolean zip =
							args.length > 1 && args[1].equalsIgnoreCase("zip");
					return requestProcessors.processResult(sessionId, zip);
				}
			}
		}
		return getMissingSessionResponse();
	}

	private NanoHTTPD.Response simulate(NanoHTTPD.IHTTPSession session,
			String uri, boolean async)
			throws IOException, NanoHTTPD.ResponseException
	{

		String sessionId = async ?
				ProcessingUtils.getSessionFromUrl(uri, SIMULATEASYNC) :
				ProcessingUtils.getSessionFromUrl(uri, SIMULATE);
		if (sessionId != null
				&& this.sessionController.containsSession(sessionId))
		{
			// Extract postdata
			Map<String, String> files = new HashMap<>();
			session.parseBody(files);
			String data = files.get("postData");
			return requestProcessors.processSimulate(sessionId, data, async);
		}
		return getMissingSessionResponse();
	}

	private NanoHTTPD.Response upload(NanoHTTPD.IHTTPSession session,
			String uri)
	{
		String sessionId = ProcessingUtils.getSessionFromUrl(uri, UPLOAD);
		if (sessionId != null
				&& this.sessionController.containsSession(sessionId))
		{
			return requestProcessors.processFileUpload(sessionId, session.getHeaders(), session.getInputStream());
		}
		return getMissingSessionResponse();
	}

	private NanoHTTPD.Response initialize(NanoHTTPD.IHTTPSession session,
			String uri) throws IOException, NanoHTTPD.ResponseException
	{
		String sessionId = ProcessingUtils.getSessionFromUrl(uri, INITIALIZE);
		if (sessionId != null
				&& this.sessionController.containsSession(sessionId))
		{
			// Extract postdata
			Map<String, String> files = new HashMap<>();
			session.parseBody(files);
			String data = files.get("postData");

			if (data == null)
			{
				ProcessingUtils.newFixedLengthPlainResponse(NanoHTTPD.Response.Status.BAD_REQUEST, "Missing post data");
			}

			return requestProcessors.processInitialize(sessionId, data);
		}
		return getMissingSessionResponse();
	}

	private NanoHTTPD.Response status(String uri)
	{
		String sessionId = ProcessingUtils.getSessionFromUrl(uri, STATUS);
		if (sessionId == null || sessionController.containsSession(sessionId))
		{
			try
			{
				return NanoWSDImpl.newFixedLengthResponse(NanoHTTPD.Response.Status.OK, Response.MIME_JSON, mapper.writeValueAsString(
						sessionId == null ?
								sessionController.getStatus() :
								sessionController.getStatus(sessionId)));
			} catch (JsonProcessingException e)
			{
				return ProcessingUtils.newFixedLengthPlainResponse(NanoHTTPD.Response.Status.INTERNAL_ERROR,
						"Internal error " + e);
			}
		} else
		{
			return getMissingSessionResponse();
		}
	}

	private NanoHTTPD.Response stopSimulation(String uri)
	{
		String sessionId = ProcessingUtils.getSessionFromUrl(uri, STOPSIMULATION);
		if (sessionId == null || sessionController.containsSession(sessionId))
		{
			sessionController.getCoe(sessionId).stopSimulation();
			return ProcessingUtils.newFixedLengthPlainResponse(NanoHTTPD.Response.Status.OK,
					"Stopping simulation with session id: " + sessionId);
		} else
		{
			return getMissingSessionResponse();
		}
	}

	private NanoHTTPD.Response pages(NanoHTTPD.IHTTPSession session)
	{
		try
		{
			return NanoHTTPD.newChunkedResponse(NanoHTTPD.Response.Status.OK, pages.get(session.getUri()).getMimeType(), pages.get(session.getUri()).load());
		} catch (Exception e)
		{
			return ProcessingUtils.newFixedLengthPlainResponse(NanoHTTPD.Response.Status.INTERNAL_ERROR,
					"Internal error " + e);
		}
	}

}
