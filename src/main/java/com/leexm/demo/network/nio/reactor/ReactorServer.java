package com.leexm.demo.network.nio.reactor;

import java.io.IOException;

/**
 * Reactor 线程模型版 服务端
 *
 * @author leexm
 * @date 2019-10-26 17:55
 */
public class ReactorServer {

    public static void main(String[] args) throws IOException {
        Reactor reactor = new Reactor(8088);
        reactor.kickOff();
    }

}
