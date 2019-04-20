### build/run/test locally

`make up`: start the dependencies:

| Application   | PORT  |
| ------------- | -----:|
| ReverseProxy  |  9443 |
| ReverseProxy  |  8080 |
| Database      |  5432 |
| LDAP          |   389 |
| EmailService  |  9010 |
| SMTP mock UI  |  8025 |
| Jaeger UI     | 16686 |
| Jaeger        |  5775 |
| Jaeger        |  6831 |
| Jaeger        |  6832 |
| Jaeger        |  5778 |
| Jaeger        | 16686 |
| Jaeger        | 14268 |
| Jaeger        |  9411 |

`sbt test it:test`: run all test

`sbt run`: start tutelar (port 9000)

[OPEN](https://lvh.me:9443/index.html) `https://lvh.me:9443/index.html`

[JAEGER](http://jaeger.lvh.me:8080) `http://jaeger.lvh.me:8080`

### for docs
```
make rundocs
```
