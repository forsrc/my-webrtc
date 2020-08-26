set NAME=my-webrtc
set PASS=forsrc
rem keytool -genkeypair -keyalg RSA -keysize 2048 -alias %NAME% -dname "CN=%NAME%,OU=forsrc,O=forsrc,C=CN" -ext "san=DNS:%NAME%" -validity 3650 -keystore %NAME%.jks -storepass %PASS% -keypass %PASS% -deststoretype pkcs12
rem keytool -alias %NAME% -exportcert -keystore %NAME%.jks -file %NAME%.cer -storepass %PASS%
rem keytool -import -alias %NAME% -keystore %JAVA_HOME%\jre\lib\security\cacerts -file %NAME%.cer -trustcacerts -storepass %PASS%


keytool -delete -alias %NAME%.server -keystore %NAME%.server.keystore -storepass %PASS%
keytool -delete -alias %NAME%.client -keystore %NAME%.server.keystore -storepass %PASS%
keytool -list -v -keystore %NAME%.server.keystore -storepass %PASS%
rem pause

rem 1.
keytool -genkey -alias %NAME%.server -dname "CN=%NAME%,OU=forsrc,O=forsrc,C=CN" -ext "san=DNS:%NAME%" -keypass %PASS% -keyalg RSA -keysize 1024 -validity 365 -keystore %NAME%.server.keystore -storepass %PASS% 
keytool -importkeystore -srckeystore %NAME%.server.keystore -destkeystore %NAME%.server.p12 -srcstoretype JKS -deststoretype PKCS12 -srcstorepass %PASS% -deststorepass %PASS% -srckeypass %PASS% -destkeypass %PASS% -srcalias %NAME%.server -destalias %NAME%.server -noprompt


rem 2.
keytool -genkey -alias %NAME%.client -dname "CN=%NAME%,OU=forsrc,O=forsrc,C=CN" -ext "san=DNS:%NAME%" -keypass %PASS% -keyalg RSA -keysize 1024 -validity 365 -storetype PKCS12 -keystore %NAME%.client.p12 -storepass %PASS%


rem 3.
keytool -export -alias %NAME%.client -keystore %NAME%.client.p12 -storetype PKCS12 -keypass %PASS% -file %NAME%.client.cer -storepass %PASS%


keytool -import -alias %NAME%.client -file %NAME%.client.cer -keystore %NAME%.server.keystore -storepass %PASS% -trustcacerts -noprompt
keytool -list -v -keystore %NAME%.server.keystore -storepass %PASS%

rem 4.  -> install
keytool -keystore %NAME%.server.keystore -export -alias %NAME%.server -file %NAME%.server.cer -storepass %PASS%

pause