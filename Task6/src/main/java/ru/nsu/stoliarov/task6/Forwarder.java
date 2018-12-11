package ru.nsu.stoliarov.task6;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class Forwarder implements Runnable {
	private static final Logger logger = LogManager.getLogger(Forwarder.class.getName());
	
	private InetSocketAddress rightAddress;
	private Selector selector;
	private ServerSocketChannel mainChannel;
	
	// write 4     0100
	// read 1      0001
	// connect 8   1000
	// accept 16  10000
	
	public Forwarder(int leftPort, String rightHost, int rightPort) throws IOException {
		rightAddress = new InetSocketAddress(rightHost, rightPort);
		selector = Selector.open();
		mainChannel = ServerSocketChannel.open();
		
		mainChannel.bind(new InetSocketAddress("localhost", leftPort));
		mainChannel.configureBlocking(false);
		mainChannel.register(selector, SelectionKey.OP_ACCEPT);
	}
	
	@Override
	public void run() {
		while(true) {
			try {
				int numberOfKeys = selector.select();
				if(0 == numberOfKeys) {
					continue;
				}
				Set<SelectionKey> keys = selector.selectedKeys();
				Iterator<SelectionKey> iterator = keys.iterator();
				
				while(iterator.hasNext()) {
					SelectionKey key = iterator.next();
					
					if(key.isAcceptable()) {
						accept();
					}
					if(key.isReadable()) {
						if(!read(key)) {
							continue;
						}
					}
					if(key.isWritable()) {
						write(key);
					}
				}
				
				selector.selectedKeys().clear();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void accept() throws IOException {
		SocketChannel clientSocketChannel = mainChannel.accept();
		clientSocketChannel.configureBlocking(false);
		SelectionKey clientKey = clientSocketChannel.register(selector, SelectionKey.OP_READ);
		
		SocketChannel serverSocketChannel = SocketChannel.open(rightAddress);
		serverSocketChannel.configureBlocking(false);
		SelectionKey serverKey = serverSocketChannel.register(selector, SelectionKey.OP_READ);
		
		Info info = new Info(clientKey, serverKey);
		clientKey.attach(info);
		serverKey.attach(info);
	}
	
	private boolean read(SelectionKey key) throws IOException {
		SocketChannel channel = (SocketChannel) key.channel();
		Info info = (Info) key.attachment();
		ByteBuffer buffer = info.getBuffer();
		SelectionKey otherKey = key.equals(info.getServerKey()) ? info.getClientKey() : info.getServerKey();
		
		int bytesNumber = channel.read(buffer);
		if(-1 == bytesNumber) {
//			logger.debug("read zero bytes from " + ((SocketChannel) key.channel()).getRemoteAddress());
			key.interestOpsAnd(0);
			return false;
		}
		
		otherKey.interestOpsOr(SelectionKey.OP_WRITE);
		if(!buffer.hasRemaining()) {
			key.interestOpsAnd(~SelectionKey.OP_READ);
		}
		return true;
	}
	
	private void write(SelectionKey key) throws IOException {
		SocketChannel channel = (SocketChannel) key.channel();
		Info info = (Info) key.attachment();
		ByteBuffer buffer = info.getBuffer();
		SelectionKey otherKey = key.equals(info.getServerKey()) ? info.getClientKey() : info.getServerKey();
		
		buffer.flip();
		channel.write(buffer);
		buffer.flip();
		
		otherKey.interestOpsOr(SelectionKey.OP_READ);
		if(0 == buffer.position()) {
			key.interestOpsAnd(~SelectionKey.OP_WRITE);
		}
	}
	
	private class Info {
		private SelectionKey clientKey;
		private SelectionKey ServerKey;
		private ByteBuffer buffer = ByteBuffer.allocate(256);
		
		Info(SelectionKey client, SelectionKey server) {
			clientKey = client;
			ServerKey = server;
		}
		
		public void setBuffer(ByteBuffer buffer) {
			this.buffer = buffer;
		}
		
		public ByteBuffer getBuffer() {
			return buffer;
		}
		
		public SelectionKey getClientKey() {
			return clientKey;
		}
		
		public void setClientKey(SelectionKey clientKey) {
			this.clientKey = clientKey;
		}
		
		public SelectionKey getServerKey() {
			return ServerKey;
		}
		
		public void setServerKey(SelectionKey serverKey) {
			ServerKey = serverKey;
		}
	}
}