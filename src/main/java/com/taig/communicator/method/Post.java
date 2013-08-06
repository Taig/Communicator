package com.taig.communicator.method;

import com.taig.communicator.event.Event;
import com.taig.communicator.event.Updateable;
import com.taig.communicator.data.Data;
import com.taig.communicator.request.Response;
import com.taig.communicator.request.Write;
import com.taig.communicator.result.Parser;

import java.io.IOException;
import java.net.URL;

import static com.taig.communicator.method.Method.*;

/**
 * The POST method is used to request that the origin server accept the entity enclosed in the request as a new
 * subordinate of the resource identified by the Request-URI in the Request-Line. POST is designed to allow a uniform
 * method to cover the following functions:
 * <ul>
 *     <li>Annotation of existing resources</li>
 *     <li>Posting a message to a bulletin board, newsgroup, mailing list, or similar group of articles</li>
 *     <li>Providing a block of data, such as the result of submitting a form, to a data-handling process</li>
 *     <li>Extending a database through an append operation</li>
 * </ul>
 *
 * The actual function performed by the POST method is determined by the server and is usually dependent on the
 * Request-URI. The posted entity is subordinate to that URI in the same way that a file is subordinate to a directory
 * containing it, a news article is subordinate to a newsgroup to which it is posted, or a record is subordinate to a
 * database.
 *
 * The action performed by the POST method might not result in a resource that can be identified by a URI. In this case,
 * either 200 (OK) or 204 (No Content) is the appropriate response status, depending on whether or not the response
 * includes an entity that describes the result.
 *
 * If a resource has been created on the origin server, the response SHOULD be 201 (Created) and contain an entity which
 * describes the status of the request and refers to the new resource, and a Location header (see section
 * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.30">14.30</a>).
 *
 * Responses to this method are not cacheable, unless the response includes appropriate Cache-Control or Expires header
 * fields. However, the 303 (See Other) response can be used to direct the user agent to retrieve a cacheable resource.
 *
 * POST requests MUST obey the message transmission requirements set out in section 8.2.
 *
 * See section <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec15.html#sec15.1.3">15.1.3</a> for security
 * considerations.
 *
 * @param <R> The {@link Response} type (created in {@link #summarize(java.net.URL, int, String, java.util.Map, Object)}).
 * @param <E> The {@link Event} type.
 * @param <T> The resource's type as generated by the supplied {@link Parser}.
 * @see Method#POST( com.taig.communicator.result.Parser, java.net.URL, com.taig.communicator.data.Data, com.taig.communicator.event.Event.Payload )
 * @see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html">http://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html</a>
 */
public abstract class Post<R extends Response, E extends Event<R>, T> extends Write<R, E, T>
{
	private Parser<T> parser;

	public Post( Parser<T> parser, URL url, Data data, E event )
	{
		super( Type.POST, url, data, event );
		this.parser = parser;
	}

	@Override
	protected T read( URL url, Updateable.Input input ) throws IOException
	{
		return parser.parse( url, input );
	}
}