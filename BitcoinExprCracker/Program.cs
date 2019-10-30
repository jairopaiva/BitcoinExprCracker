/* Criado por Jairo Paiva
 * https://github.com/jairopaiva
 * GNU GPLv3
 * */

using NBitcoin;
using System;

namespace BitcoinExprCracker
{
    class Program
    {
        static void Main(string[] args)
        {

            for (BigInteger priv = 1; priv < 2; priv++)
            {
                Key k = new Key(Utils.BigInt2Key(priv));
                byte[] PubKey = k.PubKey.Decompress().ToBytes();

                ulong[] CorrectTryes = Generator.GeneratorMethods.GetPubKeyCorrectTryes(k, 32, true);
                Console.WriteLine("PubKey = " + NBitcoin.DataEncoders.Encoders.Hex.EncodeData(PubKey));
                Console.WriteLine("PrvKey = " + NBitcoin.DataEncoders.Encoders.Hex.EncodeData(k.ToBytes()));
                Console.WriteLine("Generated Tryes = " + Generator.GeneratorMethods.TryesToString(CorrectTryes));
                Console.WriteLine("Converted Tryes to PrvKey = " + NBitcoin.DataEncoders.Encoders.Hex.EncodeData(Generator.GeneratorMethods.ConvertTryesToPrivateKey(PubKey, CorrectTryes)));
                Console.WriteLine();
            }

            Console.ReadKey();
        }
    }
}

/* Criado por Jairo Paiva
 * https://github.com/jairopaiva
 * GNU GPLv3
 * */
