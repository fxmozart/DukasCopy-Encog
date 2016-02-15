# DukasCopy-Encog
Trading with dukascopy and Encog Neural framework 2016:


Dukascopy with JForex + Encog Neural framework

-Downloads data
-Analyses via Elman networks , and makes a quick training / evaluation.

Dependencies :

Solved with maven: 

  -JForex
  -Encog
  


Run the normal TesterMain.java (don't forget to put a username & password) inside your netbeans, and it should run NeuralJava.java.

The strategy just loads data for EURUSD , places the bars into a matrix and feeds a neural network , and trains it.
When the network is trained , you can save it , then load it (like the other strategy of the other branch is doing from command line).

-You can also load the other version by adding the files inside this project (should be somewhat simpler than fiddling with libraries).



Best.


Contact: fxmozart@gmail.com
