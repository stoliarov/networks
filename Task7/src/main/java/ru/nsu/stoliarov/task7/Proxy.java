package ru.nsu.stoliarov.task7;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.xbill.DNS.*;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;

public class Proxy implements Runnable {
	private static final Logger logger = LogManager.getLogger(Proxy.class.getName());
	
	private final int BUFFER_SIZE = Settings.BUFFER_SIZE;
	
	private Selector selector;
	private ServerSocketChannel mainChannel;
	private SelectionKey dnsKey;
	
	public Proxy(int leftPort) throws IOException {
		selector = Selector.open();
		mainChannel = ServerSocketChannel.open();
		
		mainChannel.bind(new InetSocketAddress("localhost", leftPort));
		mainChannel.configureBlocking(false);
		mainChannel.register(selector, SelectionKey.OP_ACCEPT);
	}
	
	@Override
	public void run() {
		try {
			DatagramChannel asClient = DatagramChannel.open();
			asClient.configureBlocking(false);
			
			dnsKey = asClient.register(selector, SelectionKey.OP_READ);
			dnsKey.attach(new DnsRequest());
			asClient.connect(new InetSocketAddress(ResolverConfig.getCurrentConfig().server(), 53));
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (ClosedChannelException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		while(true) {
			int numberOfKeys = 0;
			try {
				numberOfKeys = selector.select();
			} catch (IOException e) {
				e.printStackTrace();
			}
			if(0 == numberOfKeys) {
				continue;
			}
			Set<SelectionKey> keys = selector.selectedKeys();
			Iterator<SelectionKey> iterator = keys.iterator();
			
			while(iterator.hasNext()) {
				try {
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
				} catch (IOException e) {
//			    	e.printStackTrace();
				}
			}
			selector.selectedKeys().clear();
		}
	}
	
	private void accept() throws IOException {
		SocketChannel clientSocketChannel = mainChannel.accept();
		clientSocketChannel.configureBlocking(false);
		SelectionKey clientKey = clientSocketChannel.register(selector, SelectionKey.OP_READ);
		
		Info info = new Info(clientKey, clientKey, true);
		clientKey.attach(info);
	}
	
	private boolean read(SelectionKey key) throws IOException {
		if(key.equals(dnsKey)) {
			return readFromDns(key);
		}
		
		SocketChannel channel = (SocketChannel) key.channel();
		Info info = (Info) key.attachment();
		ByteBuffer buffer = info.getBuffer();
		
		int bytesNumber = channel.read(buffer);
		if(-1 == bytesNumber) {
			key.interestOpsAnd(0);
			return false;
		}
		
		if(info.isSetupMode()) {
			if(0 == info.getStep()) {
				step0(key, info);
				
			} else if(1 == info.getStep()) {
				step1(key, info, buffer);
			}
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
	
	private boolean readFromDns(SelectionKey dnsKey) throws IOException {
		DatagramChannel datagramChannel = (DatagramChannel) dnsKey.channel();
		DnsRequest dnsRequest = (DnsRequest) this.dnsKey.attachment();
		ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
		
		datagramChannel.read(buffer);
		Message message = new Message(buffer.array());
		Record[] answers = message.getSectionArray(Section.ANSWER);
		if(0 == answers.length) {
			logger.debug("Got no IP-addresses from DNS-resolver");
		} else {
			String ipAddress = answers[0].rdataToString();
			String domainName = answers[0].getName().toString();
			Info info = dnsRequest.getRequests().get(domainName);
			byte[] response = new byte[info.getDomainNameLength()];
			response[0] = 5;
			response[1] = 0;
			response[2] = 0;
			response[3] = 3;
			for(int i = 4; i < info.getDomainNameLength(); i++) {
				response[i] = info.getBuffer().array()[i];
			}
			dnsKey.interestOps(SelectionKey.OP_READ);
			
			finishSetup(info.getClientKey(), info, ipAddress, info.getServerPort(), response);
		}
		
		return true;
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
		if(checkBufferLength(info, buffer)) return;
		
		if(1 == buffer.array()[3]) {
			setupIPv4(key, info, buffer);
		} else if(3 == buffer.array()[3]) {
			setupDomainName(key, info, buffer);
		} else {
			setupIPv6(key, info);
		}
	}
	
	private void setupIPv6(SelectionKey key, Info info) {
		ByteBuffer buffer;
		logger.debug("got IPv6");
		byte[] response = new byte[2];
		response[0] = 5;
		response[1] = 8;
		
		buffer = ByteBuffer.wrap(response);
		info.setBuffer(buffer);
		key.interestOps(SelectionKey.OP_WRITE);
	}
	
	private void setupDomainName(SelectionKey key, Info info, ByteBuffer buffer) {
		logger.debug("got domain name");
		String domainName = extractDomainName(buffer);
		int port = extractPort(buffer, ((int) buffer.array()[4]) + 5, 2);
		
		info.setDomainNameLength((int) buffer.array()[4] + 5 + 2);
		info.setServerPort(port);
		DnsRequest dnsRequest = (DnsRequest) dnsKey.attachment();
		dnsRequest.getRequests().put(domainName + ".", info);
		dnsRequest.getToSend().add(domainName + ".");
		dnsKey.interestOps(SelectionKey.OP_WRITE);
		key.interestOpsAnd(0);
	}
	
	private void setupIPv4(SelectionKey key, Info info, ByteBuffer buffer) throws IOException {
		logger.debug("got IPv4");
		String ipAddress = extractIpAddress(buffer);
		int port = extractPort(buffer, 8, 2);
		
		byte[] response = new byte[10];
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
		finishSetup(key, info, ipAddress, port, response);
	}
	
	private boolean checkBufferLength(Info info, ByteBuffer buffer) {
		if(buffer.array().length < 3) {
			logger.warn("Unexpected buffer length");
			return true;
		}
		return false;
	}
	
	private void finishSetup(SelectionKey key, Info info, String ipAddress, int port, byte[] response) throws IOException {
		ByteBuffer buffer;
		buffer = ByteBuffer.wrap(response);
		info.setBuffer(buffer);
		info.increaseStep();
		key.interestOps(SelectionKey.OP_WRITE);
		
		SocketChannel serverSocketChannel = SocketChannel.open(new InetSocketAddress(ipAddress, port));
		serverSocketChannel.configureBlocking(false);
		SelectionKey serverKey = serverSocketChannel.register(selector, SelectionKey.OP_READ);
		
		info.setServerKey(serverKey);
		serverKey.attach(info);
		info.setReadyToReceiveData(true);
	}
	
	private int extractPort(ByteBuffer buffer, int offset, int length) {
		StringBuilder portBuilder = new StringBuilder();
		for(int i = offset; i < offset + length; i++) {
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
//		System.out.println("port (parse) " + Integer.parseInt(portBuilder.toString(), 2));
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
	
	private String extractDomainName(ByteBuffer buffer) {
		StringBuilder domainName = new StringBuilder();
		for(int i = 5; i < buffer.array()[4] + 5; i++) {
			domainName.append((char) buffer.array()[i]);
		}
		return domainName.toString();
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
		if(key.equals(dnsKey)) {
			DatagramChannel datagramChannel = (DatagramChannel) key.channel();
			DnsRequest dnsRequest = (DnsRequest) dnsKey.attachment();
			dnsRequest.getToSend().forEach(domainName -> {
				try {
					Message message = Message.newQuery(Record.newRecord(new Name(domainName), Type.A, DClass.ANY));
					datagramChannel.write(ByteBuffer.wrap(message.toWire()));
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
			dnsRequest.getToSend().clear();

			key.interestOps(SelectionKey.OP_READ);
			return;
		}
		
		SocketChannel channel = (SocketChannel) key.channel();
		Info info = (Info) key.attachment();
		ByteBuffer buffer = info.getBuffer();
		
		
		if(info.isSetupMode()) {

			channel.write(buffer);
			buffer.clear();
			info.setBuffer(ByteBuffer.allocate(BUFFER_SIZE));
			
			if(info.isReadyToReceiveData()) {
				info.setSetupMode(false);
			}
			key.interestOps(SelectionKey.OP_READ);
			
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
}