import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class Attachment {
	private ByteBuffer length;
	private ByteBuffer buffer;
	private String message;
	private SocketAddress address;
	private SocketChannel channel;

	public Attachment(SocketChannel ch) {
		try {
			this.length = ByteBuffer.allocate(Integer.BYTES);
			this.buffer = null;
			this.message = null;
			this.address = ch.getRemoteAddress();
			this.channel = ch;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public ByteBuffer getLength() {
		return this.length;
	}

	public ByteBuffer getBuffer() {
		return this.buffer;
	}

	public void allocateBuffer(int dim) {
		this.buffer = ByteBuffer.allocate(dim);
	}

	public String getMessage() {
		return this.message;
	}

	public void setMessage(String s) {
		this.message = s;
	}

	public SocketAddress getAddress() {
		return this.address;
	}

	public void setAddress(SocketAddress addr) {
		this.address = addr;
	}

	public SocketChannel getChannel() {
		return this.channel;
	}

	public void setChannel(SocketChannel channel) {
		this.channel = channel;
	}
}