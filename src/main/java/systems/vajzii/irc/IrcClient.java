package systems.vajzii.irc;

import systems.vajzii.irc.handler.IrcClientHandler;
import systems.vajzii.irc.network.IrcPacket;
import systems.vajzii.irc.network.IrcPacketManager;
import systems.vajzii.irc.network.packets.Irc06ChangeIngameName;
import systems.vajzii.irc.network.packets.Irc09JoinServer;
import systems.vajzii.irc.network.packets.Irc21EncryptionResponse;
import systems.vajzii.irc.network.packets.Irc22VanishPlayer;
import systems.vajzii.irc.network.packets.Irc23UnvanishPlayer;
import systems.vajzii.irc.players.IrcPlayer;
import systems.vajzii.irc.util.IrcEncrypt;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.Getter;
import lombok.Setter;
import team.gothaj.client.api.util.Utils;
import team.gothaj.client.main.GothajClient;

import javax.net.ssl.*;
import java.net.URI;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

@Getter
public class IrcClient {

	private final IrcPacketManager packetManager;
	private final String uriString;

	private EventLoopGroup group;
	private Channel channel;
	@Setter
	private volatile boolean connected = false;
	private volatile boolean shouldReconnect = true;

	private boolean vanished = false;

	private final IrcEncrypt encryption;

	private final String token;
	private final IrcType type;
	private final String uuid;
	private String ingameName;
	private String serverIp;
	private String ircNick;

	public IrcClient(String host, String token, IrcType type, String uuid, String ircNick, String ingameName) {
		this.uriString = "wss://" + host + "/";
		this.token = token;
		this.type = type;
		this.uuid = uuid;
		this.ingameName = ingameName;
		this.packetManager = new IrcPacketManager();
		this.encryption = new IrcEncrypt();
		this.ircNick = ircNick;

		this.startConnectionThread();
	}

	private void startConnectionThread() {
		new Thread(() -> {
			while (shouldReconnect) {
				try {
					connect();
				} catch (Exception e) {
					e.printStackTrace(System.err);
				}

				if (shouldReconnect) {
					System.out.println("Disconnected. Reconnecting in 5s...");
					try {
						Thread.sleep(5000);
					} catch (InterruptedException ignored) {}
				}
			}
		}, "IRC-Netty-Thread").start();
	}

	private void connect() throws Exception {
		URI uri = new URI(uriString);
		String scheme = uri.getScheme() == null ? "ws" : uri.getScheme();
		final String host = uri.getHost() == null ? "127.0.0.1" : uri.getHost();
		final int port = uri.getPort() == -1 ? (scheme.equalsIgnoreCase("wss") ? 443 : 80) : uri.getPort();

		final boolean ssl = scheme.equalsIgnoreCase("wss");

		final SSLContext sslCtx;

		if (ssl) {
			try {
				TrustManager[] trustAllCerts = new TrustManager[] {
						new X509TrustManager() {
							public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
							public void checkClientTrusted(X509Certificate[] certs, String authType) {}
							public void checkServerTrusted(X509Certificate[] certs, String authType) {}
						}
				};

				sslCtx = SSLContext.getInstance("TLSv1.2");
				sslCtx.init(null, trustAllCerts, new SecureRandom());
			} catch (Exception e) {
				throw new RuntimeException("Failed to create SSL Context", e);
			}
		} else {
			sslCtx = null;
		}

		encryption.disable();

		group = new NioEventLoopGroup();
		try {
			IrcClientHandler handler = new IrcClientHandler(
					WebSocketClientHandshakerFactory.newHandshaker(
							uri, WebSocketVersion.V13, null, false,
							new DefaultHttpHeaders().add("X-IRC-Secret", "PbC6Lpn30tjZXkiM6670ZSZp")
					), this);

			Bootstrap b = new Bootstrap();
			b.group(group)
					.channel(NioSocketChannel.class)
					.handler(new ChannelInitializer<SocketChannel>() {
						@Override
						protected void initChannel(SocketChannel ch) {
							if (sslCtx != null) {
								SSLEngine engine = sslCtx.createSSLEngine(host, port);
								engine.setUseClientMode(true);
								ch.pipeline().addLast("ssl", new SslHandler(engine));
							}

							ch.pipeline().addLast(new HttpClientCodec(), new HttpObjectAggregator(8192));

							ch.pipeline().addLast(new IdleStateHandler(60, 30, 0));

							ch.pipeline().addLast(handler);
						}
					});

			ChannelFuture f = b.connect(host, port).sync();
			this.channel = f.channel();
			handler.handshakeFuture().sync();

			setConnected(true);

			if (shouldReconnect) {
				try {
					setIngameName(Utils.mc.session.getUsername());
					setMcServerIp(Utils.mc.getCurrentServerData() == null ? "In Menu" : Utils.mc.getCurrentServerData().serverIP);
				} catch (Exception ignored) {}
			}

			f.channel().closeFuture().sync();

		} finally {
			group.shutdownGracefully();
			this.connected = false;
			encryption.disable();
		}
	}

	public void disconnect() {
		this.shouldReconnect = false;
		if (this.channel != null) {
			this.channel.close();
		}
		if (this.group != null) {
			this.group.shutdownGracefully();
		}
		this.connected = false;

		encryption.disable();
	}

	public void sendPacket(IrcPacket packet) {
		if (this.channel != null && this.channel.isActive() && this.connected) {
			this.channel.writeAndFlush(packet);
		}
	}

	public byte[] generateSharedSecret() {
		byte[] secret = new byte[16];
		new SecureRandom().nextBytes(secret);
		return secret;
	}

	public void setVanish(String ircNick) {
		this.ircNick = ircNick;

		Irc22VanishPlayer packet = new Irc22VanishPlayer();
		packet.player = ircNick;
		this.sendPacket(packet);
	}

	public void toggleVanish(String ircNick, int rankID) {
		IrcPlayer currentPlayer = IrcPlayer.getPlayerByIRCUsername(ircNick);
		
		if (currentPlayer != null && currentPlayer.isVanished()) {
			setUnvanish(ircNick, rankID);
		} else {
			setVanish(ircNick);
		}
	}

	public void setUnvanish(String ircNick, int rankID) {
		this.ircNick = ircNick;

		Irc23UnvanishPlayer packet = new Irc23UnvanishPlayer();
		packet.ircName = ircNick;
		packet.ingameName = this.ingameName;
		packet.rankID = rankID;
		packet.serverIp = this.serverIp;
		packet.type = "player";
		this.sendPacket(packet);
	}

	public void setIngameName(String name) {
		this.ingameName = name;
		Irc06ChangeIngameName packet = new Irc06ChangeIngameName();
		packet.name = name;
		this.sendPacket(packet);
	}

	public void setMcServerIp(String ip) {
		this.serverIp = ip;
		Irc09JoinServer packet = new Irc09JoinServer();
		packet.server = ip;
		this.sendPacket(packet);
	}
}
