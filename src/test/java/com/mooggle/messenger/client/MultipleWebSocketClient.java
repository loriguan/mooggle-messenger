/*
 * Copyright 2014 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.mooggle.messenger.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketClientCompressionHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

import java.net.URI;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This is an example of a WebSocket client.
 * <p>
 * In order to run this example you need a compatible WebSocket server.
 * Therefore you can either start the WebSocket server from the examples by
 * running {@link io.netty.example.http.websocketx.server.WebSocketServer} or
 * connect to an existing WebSocket server such as <a
 * href="http://www.websocket.org/echo.html">ws://echo.websocket.org</a>.
 * <p>
 * The client will attempt to connect to the URI passed to it as the first
 * argument. You don't have to specify any arguments if you want to connect to
 * the example WebSocket server, as this is the default.
 */
public final class MultipleWebSocketClient {

	static final String URL = System.getProperty("url", "ws://127.0.0.1:8080/websocket");

	public static void main(String[] args) throws Exception {
		final URI uri = new URI(URL);
		String scheme = uri.getScheme() == null ? "ws" : uri.getScheme();
		final String host = uri.getHost() == null ? "127.0.0.1" : uri.getHost();
		final int port;
		if (uri.getPort() == -1) {
			if ("ws".equalsIgnoreCase(scheme)) {
				port = 80;
			} else if ("wss".equalsIgnoreCase(scheme)) {
				port = 443;
			} else {
				port = -1;
			}
		} else {
			port = uri.getPort();
		}

		if (!"ws".equalsIgnoreCase(scheme) && !"wss".equalsIgnoreCase(scheme)) {
			System.err.println("Only WS(S) is supported.");
			return;
		}

		final boolean ssl = "wss".equalsIgnoreCase(scheme);
		final SslContext sslCtx;
		if (ssl) {
			sslCtx = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
		} else {
			sslCtx = null;
		}
		long startTime = System.currentTimeMillis();
		int total = 6000;
		final EventLoopGroup group = new NioEventLoopGroup();
		final AtomicInteger count = new AtomicInteger(0);
		for (int m = 0; m < total; m++) {
			try {
				final Bootstrap b = new Bootstrap();
				b.group(group).channel(NioSocketChannel.class).handler(new ChannelInitializer<SocketChannel>() {
					@Override
					protected void initChannel(SocketChannel ch) {
						ChannelPipeline p = ch.pipeline();
						if (sslCtx != null) {
							p.addLast(sslCtx.newHandler(ch.alloc(), host, port));
						}
						p.addLast(
								new HttpClientCodec(),
								new HttpObjectAggregator(8192),
								WebSocketClientCompressionHandler.INSTANCE,
								new WebSocketClientHandler(WebSocketClientHandshakerFactory.newHandshaker(uri, WebSocketVersion.V13, null, true,
										new DefaultHttpHeaders())));
					}
				});

				Channel ch = b.connect(uri.getHost(), port).sync().channel();
				((WebSocketClientHandler) (ch.pipeline().last())).handshakeFuture().sync();
				System.out.println("Connection#" + count.incrementAndGet());
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
			}
		}
		long cost = System.currentTimeMillis() - startTime;
		System.out.println("cost:" + cost);
		System.out.println("per:" + (cost * 1.0 / total));
		System.out.println("throughput:" + (total * 1000.0 / cost));
		group.shutdownGracefully();
	}
}
