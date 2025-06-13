package artskif.trader.websocket;


import jakarta.enterprise.context.ApplicationScoped;

//@ClientEndpoint
@ApplicationScoped
public class OKXWebSocketClient {

//    private Session session;

    public void connect() {
        try {
//            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
//            container.connectToServer(this, new URI("wss://example.com/ws"));
            System.out.println("✅ WebSocket соединение установлено");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

//    @PreDestroy
    public void cleanup() {
//        if (session != null && session.isOpen()) {
//            try {
//                session.close();
//                System.out.println("✅ WebSocket соединение закрыто");
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
    }

//    @OnOpen
//    public void onOpen(Session session) {
////        this.session = session;
//        System.out.println("🔗 Подключение установлено");
//    }

//    @OnMessage
    public void onMessage(String message) {
        System.out.println("📩 Получено сообщение: " + message);
    }

//    @OnClose
//    public void onClose(Session session, CloseReason reason) {
//        System.out.println("🔌 Соединение закрыто: " + reason);
//    }

//    @OnError
//    public void onError(Session session, Throwable t) {
//        System.err.println("❌ Ошибка WebSocket: " + t.getMessage());
//    }
}
