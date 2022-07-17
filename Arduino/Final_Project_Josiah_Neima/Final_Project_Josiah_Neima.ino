#include "secrets.h"
#include <WiFiClientSecure.h>
#include <MQTTClient.h>
#include <ArduinoJson.h>
#include "WiFi.h"
#include <Wire.h>
#include <Adafruit_AHTX0.h>
#include <Adafruit_DPS310.h>
#include "Adafruit_SGP30.h"

// The MQTT topics that this device should publish/subscribe
#define AWS_IOT_PUBLISH_TOPIC   "device/1/data"
#define AWS_IOT_SUBSCRIBE_TOPIC "esp32/sub"

// Defining pin for light sensor
#define lightSensor A2

// Instantiating Wifi Secure Client and MQTTClient
WiFiClientSecure net = WiFiClientSecure();
MQTTClient client = MQTTClient(256);

// Startup variables AHT
Adafruit_AHTX0 aht;
float temperature_x = 0;
float humidity_x = 0;

// Startup variables DPS
Adafruit_DPS310 dps;
Adafruit_Sensor *dps_pressure = dps.getPressureSensor();
float pressure = 0;

// Startup variables SGP
Adafruit_SGP30 sgp;
float TVOC = 0;
float CO2 = 0;

// Startup variable light level
float lightLevel = 0;

// Count variable
int count = 0;

// Absolute Humidity function for SGP
uint32_t getAbsoluteHumidity(float temperature, float humidity) {
    // approximation formula from Sensirion SGP30 Driver Integration chapter 3.15
    const float absoluteHumidity = 216.7f * ((humidity / 100.0f) * 6.112f * exp((17.62f * temperature) / (243.12f + temperature)) / (273.15f + temperature)); // [g/m^3]
    const uint32_t absoluteHumidityScaled = static_cast<uint32_t>(1000.0f * absoluteHumidity); // [mg/m^3]
    return absoluteHumidityScaled;
}

// AWS Connection Function
void connectAWS()
{
  WiFi.mode(WIFI_STA);
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);

  Serial.println("Connecting to Wi-Fi");

  while (WiFi.status() != WL_CONNECTED){
    delay(500);
    Serial.print(".");
  }

  // Configure WiFiClientSecure to use the AWS IoT device credentials
  net.setCACert(AWS_CERT_CA);
  net.setCertificate(AWS_CERT_CRT);
  net.setPrivateKey(AWS_CERT_PRIVATE);

  // Connect to the MQTT broker on the AWS endpoint we defined earlier
  client.begin(AWS_IOT_ENDPOINT, 8883, net);

  // Create a message handler
  client.onMessage(messageHandler);

  Serial.print("Connecting to AWS IOT ");

  while (!client.connect(THINGNAME)) {
    Serial.print(".");
    delay(100);
  }

  if(!client.connected()){
    Serial.println("AWS IoT Timeout!");
    return;
  }

  // Subscribe to a topic
  client.subscribe(AWS_IOT_SUBSCRIBE_TOPIC);

  Serial.println("AWS IoT Connected!");
}

// Message publisher
void publishMessage()
{
  // JSON format
  StaticJsonDocument<200> doc;
  doc["temp"] = temperature_x;
  doc["humidity"] = humidity_x;
  doc["TVOC"] = TVOC;
  doc["CO2"] = CO2;
  doc["barometer"] = pressure;
  doc["light"] = lightLevel;
  char jsonBuffer[512];
  serializeJson(doc, jsonBuffer); // print to client

  // Publishing every 60 seconds
  if(count >= 60){
    client.publish(AWS_IOT_PUBLISH_TOPIC, jsonBuffer);
    count = 0;
  }
  else {
    count++;
  }
  client.publish("esp32/pub", jsonBuffer);
}

// Message Handler - for future use
void messageHandler(String &topic, String &payload) {
  Serial.println("incoming: " + topic + " - " + payload);

//  StaticJsonDocument<200> doc;
//  deserializeJson(doc, payload);
//  const char* message = doc["message"];
}


// Setup function
void setup() {
  // Serial setup
  Serial.begin(9600);
  //Checking that all I2C are available
  if (!aht.begin()) {
    Serial.println("Failed to find AHT");
    while (1);
  }
  if (! dps.begin_I2C()) {
    Serial.println("Failed to find DPS");
    while (1);
  }

  if (! sgp.begin()){
    Serial.println("Sensor not found :(");
    while (1);
  }
  dps.configurePressure(DPS310_64HZ, DPS310_64SAMPLES);
  sgp.setIAQBaseline(0x8D6F, 0x8E97);
  connectAWS();
}

// Reconnecting function
void reconnect() {
  // Loop until we're reconnected
  while (!client.connected()) {
    client.begin(AWS_IOT_ENDPOINT, 8883, net);
    // Create a message handler
    client.onMessage(messageHandler);
    Serial.print("Connecting to AWS IOT ");
    //client.begin(AWS_IOT_ENDPOINT, 8883, net);
    if (client.connect(THINGNAME)) {
      Serial.println("AWS IoT Connected!"); 
    } else {
      Serial.println("Failed: Retrying... ");
      delay(2000);
    }
  }
}

// Main loop
void loop() {
  //Checking if connected to AWS IoT MQTT
  if (!client.connected()) {
    connectAWS();
  }
  // Sensor declaration and read
  sensors_event_t humidity, temp, pressure_event;
  aht.getEvent(&humidity, &temp);
  dps_pressure->getEvent(&pressure_event);
  lightLevel = analogRead(lightSensor);

  // Putting sensors in globals
  temperature_x = temp.temperature;
  humidity_x = humidity.relative_humidity;
  pressure =  pressure_event.pressure;
  sgp.setHumidity(getAbsoluteHumidity(temperature_x, humidity_x));
  sgp.IAQmeasure();
  TVOC = sgp.TVOC;
  CO2 = sgp.eCO2;

  //Publishing
  publishMessage();
  client.loop();

  // Wait 1 second
  delay(1000);
}
