package com.taig.communicator.method;

import com.taig.communicator.event.Event;
import com.taig.communicator.event.Updateable;
import com.taig.communicator.data.Data;
import com.taig.communicator.request.Write;
import com.taig.communicator.result.Parser;

import java.io.IOException;
import java.net.URL;

import static com.taig.communicator.method.Method.*;

/**
 * The DELETE method requests that the origin server delete the resource identified by the Request-URI. This method MAY
 * be overridden by human intervention (or other means) on the origin server. The client cannot be guaranteed that the
 * operation has been carried out, even if the status code returned from the origin server indicates that the action has
 * been completed successfully. However, the server SHOULD NOT indicate success unless, at the time the response is
 * given, it intends to delete the resource or move it to an inaccessible location.
 * <p/>
 * A successful response SHOULD be 200 (OK) if the response includes an entity describing the status, 202 (Accepted) if
 * the action has not yet been enacted, or 204 (No Content) if the action has been enacted but the response does not
 * include an entity.
 * <p/>
 * If the request passes through a cache and the Request-URI identifies one or more currently cached entities, those
 * entries SHOULD be treated as stale. Responses to this method are not cacheable.
 *
 * @param <T> The requested resource's expected return type as generated by a supplied {@link Parser}.
 * @see Method#DELETE( com.taig.communicator.result.Parser, java.net.URL, com.taig.communicator.data.Data, com.taig.communicator.event.Event.Payload )
 * @see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html">http://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html</a>
 */
public class Delete<T> extends Write<T>
{
	private Parser<T> parser;

	public Delete( Parser<T> parser, URL url, Data data, Event.Payload<T> event )
	{
		super( Type.DELETE, url, data, event );
		this.parser = parser;
	}

	@Override
	protected T read( URL url, Updateable.Input input ) throws IOException
	{
		return parser.parse( url, input );
	}
}