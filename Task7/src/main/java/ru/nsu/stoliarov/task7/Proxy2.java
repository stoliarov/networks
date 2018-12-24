package ru.nsu.stoliarov.task7;

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

public class Proxy2 implements Runnable {
	private static final Logger logger = LogManager.getLogger(Proxy2.class.getName());
	
	private InetSocketAddress rightAddress;
	private Selector selector;
	private ServerSocketChannel mainChannel;
	
	// write 4     0100
	// read 1      0001
	// connect 8   1000
	// accept 16  10000
	
	public Proxy2(int leftPort) throws IOException {
		rightAddress = new InetSocketAddress("fit.ippolitov.me", 80);
		selector = Selector.open();
		mainChannel = ServerSocketChannel.open();
		
		mainChannel.bind(new InetSocketAddress("localhost", leftPort));
		mainChannel.configureBlocking(false);
		mainChannel.register(selector, SelectionKey.OP_ACCEPT);
	}
	
	@Override
	public void run() {
		try {
			SocketChannel asClient = SocketChannel.open();
			asClient.configureBlocking(false);
			
			SelectionKey key = asClient.register(selector, SelectionKey.OP_CONNECT);
			asClient.connect(new InetSocketAddress("fit.ippolitov.me", 80));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
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
					
					if(key.isConnectable()) {
						connect(key);
					}
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
	
	private void connect(SelectionKey key) throws IOException {
		SocketChannel client = (SocketChannel) key.channel();
		
		try {
			client.finishConnect();
		} catch (java.net.ConnectException e) {
			Info info = (Info) key.attachment();
			logger.debug("Unsuccessful try to connect to: ");
		}
		
		Info info = new Info(null, null, true);
		key.attach(info);
		byte[] response = new byte[256];
		response[0] = 5;
		response[1] = 1;
		
		info.setBuffer(ByteBuffer.wrap(response));
		System.out.println("Connected: " + client.getRemoteAddress());
		
		if(client.isConnected()) {
			key.interestOps(SelectionKey.OP_WRITE);
		} else {
			logger.warn("Connection is failed!!!");
		}
	}
	
	private void accept() throws IOException {
		System.out.println("accept");
		SocketChannel clientSocketChannel = mainChannel.accept();
		clientSocketChannel.configureBlocking(false);
		SelectionKey clientKey = clientSocketChannel.register(selector, SelectionKey.OP_READ);
		
		Info info = new Info(clientKey, clientKey, true);
		clientKey.attach(info);
	}
	
	private boolean read(SelectionKey key) throws IOException {
		SocketChannel channel = (SocketChannel) key.channel();
		Info info = (Info) key.attachment();
		ByteBuffer buffer = info.getBuffer();

//		buffer.clear();
		
		int bytesNumber = channel.read(buffer);
		System.out.println("Прочитано " + bytesNumber);
		for(byte b : buffer.array()) {
			System.out.print((int) b);
		}
		System.out.println();
		
		byte[] response = new byte[256];
		response[0] = 1;
		response[1] = 2;
		response[2] = 3;
		buffer = ByteBuffer.wrap(response);
		info.setBuffer(buffer);
		
		info.increaseStep();
		key.interestOps(SelectionKey.OP_WRITE);
		
		if(-1 == bytesNumber) {
//			logger.debug("read zero bytes from " + ((SocketChannel) key.channel()).getRemoteAddress());
			key.interestOpsAnd(0);
			return false;
		}
		return true;
	}
	
	private void write(SelectionKey key) throws IOException {
		SocketChannel channel = (SocketChannel) key.channel();
		Info info = (Info) key.attachment();
		ByteBuffer buffer = info.getBuffer();
		
		channel.write(buffer);
		System.out.println("write");
		for(byte b : buffer.array()) {
			System.out.print((int) b);
		}
		System.out.println();
		buffer.clear();
		
		
		key.interestOps(SelectionKey.OP_READ);
		
	}
	
	private class Info {
		private boolean setupMode;
		private int step;
		private SelectionKey clientKey;
		private SelectionKey ServerKey;
		private ByteBuffer buffer = ByteBuffer.allocate(256);
		
		Info(SelectionKey client, SelectionKey server, boolean setupMode) {
			clientKey = client;
			ServerKey = server;
			this.setupMode = setupMode;
			this.step = 0;
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
		
		public boolean isSetupMode() {
			return setupMode;
		}
		
		public void setSetupMode(boolean setupMode) {
			this.setupMode = setupMode;
		}
		
		public int getStep() {
			return step;
		}
		
		public void setStep(int step) {
			this.step = step;
		}
		
		public void increaseStep() {
			step++;
		}
	}
}