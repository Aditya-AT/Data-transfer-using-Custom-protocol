To build
This will also build any java files in the current directory in the container.

docker build -t javaapptest .

To create the node network
Only needs to be done once.

docker network create --subnet=172.18.0.0/16 nodenet

To Run (for example, node 1)
This will ultimately run the java Main class as an application. Initialize the server.

docker run -it -p 8080:8080 --cap-add=NET_ADMIN --net nodenet --ip 172.18.0.22 javaapptest

To Run (node 2):
Initialize the client which sends the image file (test.png)

docker run -it -p 8081:8080 --cap-add=NET_ADMIN --net nodenet --ip 172.18.0.21 javaapptest 172.18.0.22 test.jpg

To randomly drop 10% of incoming packets to Node 1:
curl "http://localhost:8080/?indrop=0.1"
