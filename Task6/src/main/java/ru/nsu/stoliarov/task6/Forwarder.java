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
import java.util.*;

public class Forwarder implements Runnable {
	private static final Logger logger = LogManager.getLogger(Forwarder.class.getName());
	
	private InetSocketAddress rightAddress = null;
	private Selector selector = null;
	private HashMap<InetSocketAddress, SelectionKey> rightKeys;
	
	private HashMap<InetSocketAddress, Info> clients;
	
	/**
	 * Channel for registration a new input connections
	 */
	private ServerSocketChannel mainChannel = null;
	
	public Forwarder(int leftPort, String rightHost, int rightPort) {
		try {
			this.clients = new HashMap<>();
			this.rightKeys = new HashMap<InetSocketAddress, SelectionKey>();
			this.selector = Selector.open();
			this.mainChannel = ServerSocketChannel.open();
			this.mainChannel.configureBlocking(false);
			this.rightAddress = new InetSocketAddress(rightHost, rightPort);
			this.mainChannel.bind(new InetSocketAddress("localhost", leftPort));
			
			// todo возможно надо после каждого успешного аксепта регистрировать интерес на новый аксепт, чтобы ждать новых клиентов
			this.mainChannel.register(selector, SelectionKey.OP_ACCEPT);
			
//			SocketChannel asClient = SocketChannel.open();
//			asClient.configureBlocking(false);
//			asClient.register(selector, SelectionKey.OP_CONNECT);
//			asClient.connect(rightAddress);
//			System.out.println("right: " + (InetSocketAddress) rightAddress);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void run() {
		while(true) {
			try {
				int numberOfKeys = selector.select();
				
				if(0 == numberOfKeys) {
					continue;
				}
				
				// gets the keys corresponding to activity
				Set keys = selector.selectedKeys();
				Iterator iterator = keys.iterator();
				
				// work with each active client
				while(iterator.hasNext()) {
					SelectionKey key = (SelectionKey) iterator.next();
					
					if(key.isAcceptable()) {
						accept(key);
					} else if(key.isConnectable()) {
						if(!connect(key)) return;
					} else if(key.isReadable()) {
						read(key);
					} else if(key.isWritable()) {
						write(key);
					}
					
					iterator.remove();  // most important!!!
				}
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private boolean connect(SelectionKey key) throws IOException {
		SocketChannel socketChannel = (SocketChannel) key.channel();
		Info serverInfo = (Info) key.attachment();
		
		try {
			socketChannel.finishConnect();
		} catch (java.net.ConnectException e) {
			logger.debug("Unsuccessful try to connect to right address: " + rightAddress);
			key.cancel();
			return false;
		}
		
		SelectionKey clientKey = serverInfo.getChannel().register(selector, SelectionKey.OP_READ);
		serverInfo.setKey(clientKey);
		
		Info clientInfo = clients.get(serverInfo.getAddress());
		clientInfo.setKey(clientKey);
		clientKey.attach(clientInfo);
		
		SelectionKey serverKey = socketChannel.register(selector, SelectionKey.OP_READ, serverInfo);
		serverKey.attach(serverInfo);
		this.rightKeys.put(serverInfo.getAddress(), serverKey);
		
		logger.debug("Connected to right address: " + socketChannel.getRemoteAddress());
		return true;
	}
	
	private boolean accept(SelectionKey key) throws IOException {
		SocketChannel clientSocketChannel = mainChannel.accept();
		
		if(null == clientSocketChannel) {
			logger.debug("Client felled of. NULL == clientSocketChanel");
			key.cancel();
			return false;
		}
		
		clientSocketChannel.configureBlocking(false);
		
		Info clientInfo = new Info((InetSocketAddress) clientSocketChannel.getRemoteAddress(), clientSocketChannel);
		clients.put(clientInfo.getAddress(), clientInfo);
		
		Info serverInfo = new Info((InetSocketAddress) clientSocketChannel.getRemoteAddress(), clientSocketChannel);
		SocketChannel asClient = SocketChannel.open();
		asClient.configureBlocking(false);
		SelectionKey serverKey = asClient.register(selector, SelectionKey.OP_CONNECT);
		serverKey.attach(serverInfo);
		asClient.connect(rightAddress);
		
		logger.debug("Accepted: " + clientSocketChannel.getRemoteAddress());
		return true;
	}
	
	int number = 0;
	
	private void read(SelectionKey key) throws IOException {
		SocketChannel channel = (SocketChannel) key.channel();
		ByteBuffer buffer = ByteBuffer.allocate(512);
//		String inMessage = new String(buffer.array()).trim();
		Info info = (Info) key.attachment();
		
//		logger.debug("Received: " + inMessage);
		
//		if(inMessage.equals("Bye")) {
//			channel.close();
//			logger.debug("Chanel is closed");
//		} else if(inMessage.equals("")) {
//			logger.debug("Got an empty string from " + channel.getRemoteAddress() + " " + buffer.array().length);
//			return;
//		}
		
		if(channel.getRemoteAddress().equals(rightAddress)) { // got from site
//			logger.debug("got from site");
			number = channel.read(buffer);
			if(-1 == number) {
//				key.cancel();
//				clients.get(info.getAddress()).getKey().cancel();
//				rightKeys.remove(info.getAddress());
//				clients.remove(info.getAddress());
				return;
			};
//			System.out.println("server" + number);
			Info clientInfo = clients.get(info.getAddress());
			clientInfo.setData(buffer.array());
			clientInfo.getKey().interestOps(SelectionKey.OP_WRITE);
		} else { // got from client
//			logger.debug("got from client");
			number = channel.read(buffer);
			if(-1 == number) {
//				key.cancel();
//				rightKeys.get(info.getAddress()).cancel();
//				rightKeys.remove(info.getAddress());
//				clients.remove(info.getAddress());
				return;
			};
			Info serverInfo = (Info) rightKeys.get(info.getAddress()).attachment();
			serverInfo.setData(buffer.array());
			rightKeys.get(info.getAddress()).interestOps(SelectionKey.OP_WRITE);
		}
		buffer.flip();
//		buffer.clear();
	}
	
	private void write(SelectionKey key) throws IOException {
		Info info = (Info) key.attachment();
		SocketChannel channel = (SocketChannel) key.channel();
		ByteBuffer buffer = ByteBuffer.wrap(info.getData());
		
		
		if(channel.getRemoteAddress().equals(rightAddress)) { // write to site
//			logger.debug("write to site");
			channel.write(buffer);
			
			info.removeData();
			key.interestOps(SelectionKey.OP_READ);
		} else { // write to client
//			logger.debug("write to client");
			channel.write(buffer);
			info.removeData();
			key.interestOps(SelectionKey.OP_READ);
		}
//		buffer.flip();
		buffer.clear();
		
//		String outMessage = "Hello from " + rightAddress.getHostString() + ":" + rightAddress.getPort();
//
//		byte[] message = outMessage.getBytes();
//		ByteBuffer buffer = ByteBuffer.wrap(message);
//
//		SocketChannel client = (SocketChannel) key.channel();
//		client.write(buffer);
//
//		logger.debug("Sent: " + outMessage);
//
//		buffer.clear();
//
//		key.interestOps(SelectionKey.OP_READ);
	}
	
	private class Info {
		private InetSocketAddress address;
		private byte[] data = null;
		private SocketChannel channel;
		private SelectionKey key;
		
		
		public Info(InetSocketAddress address, SelectionKey key, byte[] data) {
			this(address, key);
			this.data = data;
		}
		
		public Info(InetSocketAddress address, SelectionKey key) {
			this.address = address;
			this.key = key;
		}
		
		public Info(InetSocketAddress address, SocketChannel channel) {
			this.address = address;
			this.channel = channel;
		}
		
		public Info(InetSocketAddress address) {
			this.address = address;
		}
		
		public SelectionKey getKey() {
			return key;
		}
		
		public void setKey(SelectionKey key) {
			this.key = key;
		}
		
		public SocketChannel getChannel() {
			return channel;
		}
		
		public void setChannel(SocketChannel channel) {
			this.channel = channel;
		}
		
		public boolean hasData() {
			return null != data;
		}
		
		public InetSocketAddress getAddress() {
			return address;
		}
		
		public void setAddress(InetSocketAddress address) {
			this.address = address;
		}
		
		public byte[] getData() {
			return data;
		}
		
		public void removeData() {
			data = null;
		}
		
		public void setData(byte[] data) {
			this.data = data;
		}
	}
}
