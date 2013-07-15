package com.taig.communicator.result;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public interface Parser<T>
{
	public abstract T parse( URL url, InputStream stream ) throws IOException;
}