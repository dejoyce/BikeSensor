/*
  Ultrasonic Sensor HC-SR04 and Arduino Tutorial

  Crated by Dejan Nedelkovski,
  www.HowToMechatronics.com

*/

#include "pitches.h"
#include <LiquidCrystal.h>

// defines pins numbers
const int trigPin = 4;
const int echoPin = 3;
const int speakerPin = 5;
const int ledPin = 2;
const int tiltSensorPin = 13; 
const int emergencyButtonPin = 10;




// defines variables
long duration;
int distance;

int melody[] = {   NOTE_C4, NOTE_G3, NOTE_G3, NOTE_A3, NOTE_G3, 0, NOTE_B3, NOTE_C4 };
// note durations: 4 = quarter note, 8 = eighth note, etc.:
int noteDurations[] = {   4, 8, 8, 4, 4, 4, 4, 4 };

LiquidCrystal lcd(12, 11, 6, 7, 8, 9);

int tiltSensorState = 0;         // variable for reading the tilt sensor
int emergencyButtonState = 0;         // variable for reading the emergency pushbutton status


void setup() 
{
  pinMode(speakerPin, OUTPUT); // Sets the trigPin as an Output
  pinMode(trigPin, OUTPUT); // Sets the trigPin as an Output
  pinMode(ledPin, OUTPUT);
  pinMode(echoPin, INPUT); // Sets the echoPin as an Input

    pinMode(emergencyButtonPin, INPUT); // Sets the trigPin as an Output
    
  Serial.begin(9600); // Starts the serial communication

  pinMode(tiltSensorPin, INPUT);

   lcd.begin(16, 2);

   lcd.print("Distance!");

   Serial.begin(9600);
}

void loop() 
{  

        
  if(Serial.available())  
  {  
   char c = Serial.read();  
  // Serial.print(c);  

   if( c == 'a')
   {
          digitalWrite(speakerPin, HIGH);
          delay( 3000 );
          digitalWrite(speakerPin, LOW);
   }   
   else if( c == 'b')
   {
          digitalWrite(ledPin, HIGH);
          delay( 3000 );
          digitalWrite(ledPin, LOW);
   }
      else if( c == 'c')
   {
      lcd.setCursor(0, 0);
      lcd.print("Stayin alive @ 1.5    ");
      lcd.setCursor(0, 1);
      lcd.print("From the phone app ");       

                             delay( 3000 );
   }
         else if( c == '1')
   {
 //   Serial.print(c);  
   }
            else if( c == '2')
   {
 //   Serial.print(c);  
   }
            else if( c == '3')
   {
 //   Serial.print(c);  
   }
   
  }  



  
 // Serial.print("hello");
  // Clears the trigPin


  
  digitalWrite(trigPin, LOW);     // Write a HIGH or a LOW value to a digital pin.
  delayMicroseconds(2);           // Pauses the program for the amount of time (in microseconds) 
                                  // There are a thousand microseconds in a millisecond, and a million microseconds in a second
  
  // Sets the trigPin on HIGH state for 10 micro seconds
  digitalWrite(trigPin, HIGH);
  delayMicroseconds(10);
  digitalWrite(trigPin, LOW);
  
  // Reads the echoPin, returns the sound wave travel time in microseconds
  duration = pulseIn(echoPin, HIGH);  // Reads a pulse (either HIGH or LOW) on a pin
  
  // Calculating the distance
  distance = duration * 0.034 / 2;

           //    Serial.println( distance );

  
  // Prints the distance on the Serial Monitor  
  //Serial.print("Distance: ");
  //Serial.println(distance);

    lcd.setCursor(0, 1);



 tiltSensorState = digitalRead(tiltSensorPin);

    if (tiltSensorState == HIGH) 
    {
            lcd.setCursor(0, 0);
      lcd.print(" Tilt Sensor ON ");
    // turn LED on:
     // Serial.println( "tilt HIGH1" );
     // digitalWrite(tiltLedPin, HIGH);
      delay( 2500 );
      
      tiltSensorState = digitalRead(tiltSensorPin);
      if (tiltSensorState == HIGH) 
      {
            //  Serial.println( "tilt HIGH2" );


            
        delay( 2500 );

        if (tiltSensorState == HIGH) 
        {
              Serial.print('t');  
          //              Serial.println( "tilt HIGH3-play" );
          tone(speakerPin, 200, 500);
                   //     Serial.println( "tilt HIGH4-stop" );

      lcd.setCursor(0, 0);
      lcd.print("   Emergency   ");
      lcd.setCursor(0, 1);
      lcd.print("Contact Alerted");     
           delay( 4000 );
                   
        }
      }

//      digitalWrite(tiltLedPin, LOW);
    } 
    else
    {
   //   Serial.println( "tilt LOW" );
    }
   




   // playTone(750, 500);
  //  delay(750);

  if( distance < 4 )
  {
    digitalWrite(ledPin, HIGH);
         tone(speakerPin, 2000, 50);
         //    Serial.println("z2");

             lcd.setCursor(0, 0);

   lcd.print("  !!!Danger!!");
       lcd.setCursor(0, 1);
                     lcd.print("Car at 4 meters ");

                               delay( 200 );

     
                 Serial.print('d');  
                    
    
  }
    else if ( distance < 7 )
  {
    digitalWrite(ledPin, HIGH);
    
    // tone(speakerPin, 4435, 1000);
          //   Serial.println("z3");

     tone(speakerPin, 2000, 50);
                  //Serial.println("444444");
    lcd.setCursor(0, 0);

   lcd.print("   Danger");
       lcd.setCursor(0, 1);
                     lcd.print("Car at 4 meters ");

                               delay( 200 );

  } // END OF if
  

  else if ( distance < 10 )
  {
    digitalWrite(ledPin, HIGH);
    
    // tone(speakerPin, 4435, 1000);
            // Serial.println("z4");

     tone(speakerPin, 2000, 50);

               //   Serial.println("66666");
    lcd.setCursor(0, 0);

   lcd.print("    Warning     ");
       lcd.setCursor(0, 1);
                     lcd.print("Car at 6 meters ");

                               delay( 300 );

  } // END OF if
    else if ( distance < 18 )
  {
    digitalWrite(ledPin, HIGH);
    
    // tone(speakerPin, 4435, 1000);
             //Serial.println("10000");

               //           Serial.println("z5");

     tone(speakerPin, 2000, 50);
      lcd.setCursor(0, 0);

   lcd.print("    Caution                ");
       lcd.setCursor(0, 1);
                     lcd.print("Car at 12 meters ");
                  delay( 400 );

  } // END OF if
  else
  {
        lcd.setCursor(0, 0);

   lcd.print("No Car Detected");

           lcd.setCursor(0, 1);

   lcd.print("                   ");
 
    digitalWrite(ledPin, LOW);

            //     Serial.println("z6");
  }

  delay( 50 );
}

void playTone(long duration, int freq) {
    duration *= 1000;
    int period = (1.0 / freq) * 1000000;
    long elapsed_time = 0;
    while (elapsed_time < duration) {
        digitalWrite(speakerPin,HIGH);
        delayMicroseconds(period / 2);
        digitalWrite(speakerPin, LOW);
        delayMicroseconds(period / 2);
        elapsed_time += (period);
    }
}
