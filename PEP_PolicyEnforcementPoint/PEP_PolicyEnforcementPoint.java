import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;

public class PEP_PolicyEnforcementPoint {

	public static void main(String[] args) {
		String HOST = "localhost";
		int PORT = 44444;
		int CH_PORT = 55555;
		
		ArrayList<User> waitingClients = new ArrayList<User>();
		InetSocketAddress address = new InetSocketAddress(HOST, CH_PORT);
		
		try(ServerSocketChannel server = ServerSocketChannel.open();
				SocketChannel ch_server = SocketChannel.open(address);
				Selector selector = Selector.open();) {

			server.bind(new InetSocketAddress(HOST, PORT));
			server.configureBlocking(false);
			ch_server.configureBlocking(false);
			System.out.println("Server TCP avviato sulla porta " + PORT + System.lineSeparator());
			
			Attachment attCH = new Attachment(ch_server);

			server.register(selector, SelectionKey.OP_ACCEPT);
			ch_server.register(selector, SelectionKey.OP_READ, attCH);

			LinkedBlockingQueue<SelectionKey> bfs = new LinkedBlockingQueue<SelectionKey>();

			Worker r = new Worker(bfs, selector, waitingClients, ch_server);
			Thread rr = new Thread(r);
			rr.start();

			while(true) {
				selector.selectedKeys().clear();
				//System.out.println("Attesa...");
				selector.select();

				for(SelectionKey key : selector.selectedKeys()) {
					if(key.isAcceptable()) {
						ServerSocketChannel channel = (ServerSocketChannel) key.channel();
						SocketChannel client = channel.accept();
						client.configureBlocking(false);

						Attachment att = new Attachment(client);
						client.register(selector, SelectionKey.OP_READ, att);

						//System.out.println("Nuovo client connesso: " + client.getRemoteAddress());
					}

					if(key.isReadable()) {
						SocketChannel channel = (SocketChannel) key.channel();
						Attachment att = (Attachment) key.attachment();

						long response = channel.read(att.getLength());

						if(response == -1) {
							//System.out.println("Client " + channel.getRemoteAddress() + " disconnesso");

							channel.close();
							key.cancel();

							break;
						}
						//System.out.println("Leggo dalla socket di " + channel.getRemoteAddress());

						if (!att.getLength().hasRemaining()) {
							att.getLength().flip();
							att.allocateBuffer(att.getLength().getInt());

							long res = channel.read(att.getBuffer());

							if(res == -1) {
								//System.out.println("Client " + channel.getRemoteAddress() + " disconnesso");

								channel.close();
								key.cancel();

								break;
							}

							if (!att.getBuffer().hasRemaining()) {
								att.getBuffer().flip();
								bfs.put(key);
							}
						}

						key.interestOps(SelectionKey.OP_READ);
					}

					if(key.isWritable()) {
						SocketChannel channel = (SocketChannel) key.channel();
						Attachment att = (Attachment) key.attachment();

						//System.out.println("Scrivo sulla socket di " + channel.getRemoteAddress());

						att.allocateBuffer(att.getMessage().length());
						att.getLength().putInt(att.getMessage().length());
						att.getBuffer().put(att.getMessage().getBytes());

						att.getLength().flip();
						att.getBuffer().flip();

						channel.write(att.getLength());
						channel.write(att.getBuffer());

						att.getLength().clear();
						att.getBuffer().clear();
						
						key.interestOps(SelectionKey.OP_READ);
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}