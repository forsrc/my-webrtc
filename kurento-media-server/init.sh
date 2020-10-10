docker run --name kurento-media-server \
    -p 8888:8888/tcp \
    -p 5000-5050:5000-5050/udp \
    -e KMS_MIN_PORT=5000 \
    -e KMS_MAX_PORT=5050 \
    kurento/kurento-media-server:latest

docker run -d -p 3478:3478 -p 5347:5347 -p 49160-49200:49160-49200/udp --network=host --name coturn  --hostname coturn.forsrc.com \
    forsrc/coturn -n --log-file=stdout -f -v -r coturn.forsrc.com\
    --external-ip='$(curl -4 https://icanhazip.com 2>/dev/null)' \
    --min-port=49160 --max-port=49200 \
    -c /etc/turnserver.conf

sudo docker exec --user root -it coturn sh -c "turnadmin -a -u forsrc -p forsrc -r coturn.forsrc.com && turnadmin -l"
sudo docker exec --user root -it coturn sh -c "turnadmin -A -u forsrc -p forsrc -r coturn.forsrc.com && turnadmin -L"

docker exec -it kurento-media-server bash
# vim /etc/kurento/modules/kurento/WebRtcEndpoint.conf.ini
# turnURL=forsrc:fosrc@172.30.175.197:3478?transport=udp
# externalAddress=172.30.175.197
