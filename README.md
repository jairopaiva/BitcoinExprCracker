# BitcoinExprCracker

Este projeto é fruto de um estudo pessoal sobre o algoritmo Secp256k1. O objetivo dele é conseguir obter, usando apenas os 
valores da chave pública, o valor correto da chave privada.

O algortimo implementado, de forma pseudo-aleatória, gera expressões matemáticas usando os valores da chave pública que 
resultem no correto valor da chave privada em determinada posição.

Na figura abaixo um exemplo do algoritmo mostrando as expressões geradas para cada byte na chave privada:

![alt text](https://github.com/jairopaiva/BitcoinExprCracker/blob/master/Main_MostrarExprs.PNG)

Resultado usando como chave privada os valores de 1 à 5:

![alt text](https://github.com/jairopaiva/BitcoinExprCracker/blob/master/Main.PNG)
