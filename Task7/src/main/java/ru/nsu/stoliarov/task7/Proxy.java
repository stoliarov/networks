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

public class Proxy implements Runnable {
	private static final Logger logger = LogManager.getLogger(Proxy.class.getName());
	
	private final int BUFFER_SIZE = 8192;
	
	private Selector selector;
	private ServerSocketChannel mainChannel;
	
	// write 4     0100
	// read 1      0001
	// connect 8   1000
	// accept 16  10000
	
	public Proxy(int leftPort) throws IOException {
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
//				e.printStackTrace();
			}
		}
	}
	
	private void accept() throws IOException {
//		System.out.println("accept");
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
//		System.out.println("Прочитано " + bytesNumber);
		if(-1 == bytesNumber) {
//			logger.debug("read zero bytes from " + ((SocketChannel) key.channel()).getRemoteAddress());
			key.interestOpsAnd(0);
			return false;
		}
		
		if(info.isSetupMode()) {
//			System.out.println("step: " + info.step);
//			for(byte b : buffer.array()) {
//				System.out.print((int) b + " ");
//			}
//			System.out.println();
			
			if(0 == info.getStep()) {
				step0(key, info);
				
			} else if(1 == info.getStep()) {
				step1(key, info, buffer);
				
			} else if(2 == info.getStep()) {
				// todo
			}
//			buffer.clear();
			
			return true;
		} else {
			SelectionKey otherKey = key.equals(info.getServerKey()) ? info.getClientKey() : info.getServerKey();
			
			otherKey.interestOpsOr(SelectionKey.OP_WRITE);
			if(!buffer.hasRemaining()) {
				key.interestOpsAnd(~SelectionKey.OP_READ);
			}
			return true;
		}
	}
	
	private void step0(SelectionKey key, Info info) {
		ByteBuffer buffer;
		byte[] response = new byte[2];
		response[0] = 5;
		response[1] = 0;
		buffer = ByteBuffer.wrap(response);
		info.setBuffer(buffer);
		
		info.increaseStep();
		key.interestOps(SelectionKey.OP_WRITE);
	}
	
	private void step1(SelectionKey key, Info info, ByteBuffer buffer) throws IOException {
		if(1 == buffer.array()[3]) {    // got IPv4 address
			String ipAddress = extractIpAddress(buffer);
			int port = extractPort(buffer);
			
			byte[] response = new byte[10]; // todo размер
			response[0] = 5;
			response[1] = 0;
			response[2] = 0;
			response[3] = 1;
			for(int i = 4; i < 8; i++) {
				response[i] = buffer.array()[i];
			}
			for(int i = 8; i < 10; i++) {
				response[i] = buffer.array()[i];
			}
			
			buffer = ByteBuffer.wrap(response);
			info.setBuffer(buffer);
			info.increaseStep();
			key.interestOps(SelectionKey.OP_WRITE);
			
			SocketChannel serverSocketChannel = SocketChannel.open(new InetSocketAddress(ipAddress, port));
			serverSocketChannel.configureBlocking(false);
			SelectionKey serverKey = serverSocketChannel.register(selector, SelectionKey.OP_READ);
			
			info.setServerKey(serverKey);
			serverKey.attach(info);
			
		} else if(3 == buffer.array()[3]) {     // got domain name
			// todo асинхронный резолвинг имен
			
		} else {
			// todo отправить ответ, что тип адреса не поддерживается
			logger.debug("Браузер пытается использовать IPv6. Не поддерживается!");
		}
	}
	
	private int extractPort(ByteBuffer buffer) {
		StringBuilder portBuilder = new StringBuilder();
		for(int i = 8; i < 10; i++) {
			String pl = null;
			if(buffer.array()[i] < 0) {
				pl = extraCode(buffer.array()[i]);
			} else {
				StringBuilder plBuilder = new StringBuilder(Integer.toString(buffer.array()[i], 2));
				while(plBuilder.length() < 8) {
					plBuilder.insert(0, "0");
				}
				pl = plBuilder.toString();
			}
			portBuilder.append(pl);
		}
//		System.out.println("port " + Integer.parseInt(portBuilder.toString(), 2));
		return Integer.parseInt(portBuilder.toString(), 2);
	}
	
	private String extractIpAddress(ByteBuffer buffer) {
		StringBuilder ipAddress = new StringBuilder();
		for(int i = 4; i < 8; i++) {
			if(buffer.array()[i] < 0) {
				String extraCode = extraCode(buffer.array()[i]);
				int ipItem = Integer.parseInt(extraCode, 2);
				ipAddress.append(ipItem);
			} else {
				ipAddress.append(buffer.array()[i]);
			}
			if(7 != i) {
				ipAddress.append(".");
			}
		}
//		System.out.println("host " + ipAddress.toString());
		return ipAddress.toString();
	}
	
	private String extraCode(int number) {
		int plusBin = number * (-1);
		plusBin -= 1;
		StringBuilder plBuilder = new StringBuilder(Integer.toString(plusBin, 2));
		while(plBuilder.length() < 8) {
			plBuilder.insert(0, "0");
		}
		String pl = plBuilder.toString();
		pl = pl.replace('0', '2');
		pl = pl.replace('1', '0');
		pl = pl.replace('2', '1');
		return pl;
	}
	
	private void write(SelectionKey key) throws IOException {
		SocketChannel channel = (SocketChannel) key.channel();
		Info info = (Info) key.attachment();
		ByteBuffer buffer = info.getBuffer();
		
		if(info.isSetupMode()) {
			channel.write(buffer);
//			System.out.println("write");
//			for(byte b : buffer.array()) {
//				System.out.print((int) b);
//			}
//			System.out.println();

			buffer.clear();
			info.setBuffer(ByteBuffer.allocate(BUFFER_SIZE));
			
			if(2 == info.getStep()) {
				info.setSetupMode(false);
				key.interestOps(SelectionKey.OP_READ);
			} else {
				key.interestOps(SelectionKey.OP_READ);
			}
		} else {
			SelectionKey otherKey = key.equals(info.getServerKey()) ? info.getClientKey() : info.getServerKey();
			
			buffer.flip();
			channel.write(buffer);
			buffer.flip();
			
			otherKey.interestOpsOr(SelectionKey.OP_READ);
			if(0 == buffer.position()) {
				key.interestOpsAnd(~SelectionKey.OP_WRITE);
			}
		}
	}
	
	private class Info {
		private boolean setupMode;
		private int step;
		private SelectionKey clientKey;
		private SelectionKey ServerKey;
		private ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
		
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