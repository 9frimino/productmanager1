package productmanager.server;

//import network.chat.SocketClient;
import org.json.JSONArray;
import org.json.JSONObject;
import productmanager.Product;
import productmanager.RequestCode;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProductServer {
    private ServerSocket serverSocket;
    private ExecutorService threadPool;
    private List<Product> products;
    private int sequence;

    public static void main(String[] args) {
        ProductServer productServer = new ProductServer();
        try {
            productServer.start();
        } catch (IOException e) {
            System.out.println(e.getMessage());
 //           productServer.stop();
        }
    }


    public void start() throws IOException {
        //소켓에 포트 바인딩... 핸드폰에 번호 부여하는 것처럼 운영체제에서도 이 프로그램에서도 이걸 쓸거다라고 지정하는거다. 핸드폰 번호 중복되면 못쓰니 포트도 마찬가지다. pc에 하나만 사용 가능 이걸 바인딩됐다라고함.
        serverSocket = new ServerSocket(50001);
        threadPool = Executors.newFixedThreadPool(100); // 멀티스레드고
        products = new Vector<>();

        products.add(
                new Product(sequence++,  "삼다수", 1000,  20)
        );

        System.out.println("[서버] 시작");

        while(true) { ////이제 해당 클라이언트와 리턴된 소켓을 통해 서버와 TCP 통신을 할 수 있다.
            //연결 수락 accept 블로킹 매서드 얘가 어셉트하고 있다. 우리는 클라이언트다. 클라이언트의 전화를 기다린다. 50001의 전화를 기다린다. 받을 준비하는거임. 연결정보의 소켓을 만든거다... 이걸 갖고 게속 통신하는 거다.
            Socket socket = serverSocket.accept();
            SocketClient sc = new SocketClient(socket); //소켓클라이언트는 기능을 더 추가한거다. 소켓을 더 추가적인 기능을 사용하려고.
        }
    }

    public class SocketClient {
        private Socket socket;
        // 해당 클라이언트로부터 요청을 받을 때 사용 이거는 인풋
        private DataInputStream dis;
        //해당 클라이언트로 응답을 보낼 때 사용 이거는 아웃풋
        private DataOutputStream dos;
        private JSONObject request;

        // 밑에 소켓은 매개변수 저거는 그냥 임시적으로 넣는 값임 그래서 위에 소캣이랑 밑에 소켓이랑 같은게 아님. // 보조를 붙인 이유다 이게
        public SocketClient(Socket socket) {
            try {
                this.socket = socket;
                this.dis = new DataInputStream(socket.getInputStream());
                this.dos = new DataOutputStream(socket.getOutputStream());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        // 요청 (클라이언트가 서버에게 요청한거임.)
        // 요청이 오면 데이터를 읽고 어떤 요청인지 확인. 데이터(JSON)는 제이슨 형식으로 올 거임.
        // 요 데이터가 어디에 오냐? dis를 쓰면 됨.
        // crud list = read, create, update, delete

        public void receive() {
            threadPool.execute(() -> {
                try {
                    String receiveJson = dis.readUTF();// 얘로 읽으면 됨. 리비스제이슨갖고 뭘하면 되냐? 제이슨 저거 파싱해서 와야함.
                    //String  ㅡ> 객체화 시켜서 사용 역직렬화를 해야 하기 때문에 주고받을 때는 string 쓸 때는 obj
                    JSONObject request = new JSONObject(receiveJson);
                    int menu = request.getInt("menu");

                    switch(menu) {
                        case RequestCode.READ:
                            list();
                            break;
                        case RequestCode.UPDATE:
                            read();
                            break;
                        case RequestCode.DELETE:
                            delete();
                            break;
                        case RequestCode.CREATE:
                            create(request);
                            break;
                    }
                } catch (IOException e) {}
                close();
            });
        }

        public void list() throws IOException {
            JSONArray data = new JSONArray();
            for (Product p : products) {
                JSONObject product = new JSONObject();
                product.put("no", p.getNo());
                product.put("name", p.getName());
                product.put("menu", RequestCode.READ);
            }
        }
        private void read() {
        }

        public void create(JSONObject request) throws IOException {
            //data 안에 상품입력, 가격, 재고수량
            JSONObject data = request.getJSONObject("data");
            Product product = new Product();
            product.setNo(sequence++);
            product.setName(data.getString("name"));
            product.setPrice(data.getInt("price"));
            product.setStock(data.getInt("stock"));

            products.add(product);

            //response 보내기
            //1. JSON만들기
            JSONObject response = new JSONObject();
            response.put("status", "success");
            response.put("data", "");
            //2. 직렬화하기(문자열화)
            dos.writeUTF(response.toString());
            dos.flush();
        }

        public void delete() throws IOException {
            // 데이터 읽고
            JSONObject data = request.getJSONObject("data");
            int no = data.getInt("no");

            Iterator<Product> iterator = products.iterator();
            while(iterator.hasNext()) {
                Product product = iterator.next();
                if (product.getNo() == no) {
                    iterator.remove();
                }
            }


        }
        public void update(JSONObject request) throws IOException {
            JSONObject data = request.getJSONObject("data");
            int no = data.getInt("no");
            // 해당 no이 products 안에 있는지 확인하고 해당 상품 수정
            Iterator<Product> iterator = products.iterator();
            while (iterator.hasNext()) {
                Product product = iterator.next();
                if(product.getNo() == no) {
                    product.setName(data.getString("name"));
                    product.setPrice(data.getInt("price"));
                    product.setStock(data.getInt("stock"));
                }
            }
            JSONObject response = new JSONObject();
            response.put("status", "success");
            response.put("data", new JSONObject());
            dos.writeUTF(response.toString());
            dos.flush();
        }

        public void close() {
            try {
                socket.close();
            } catch(Exception e) {}
        }
    }
}

