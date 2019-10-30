/* Criado por Jairo Paiva
 * https://github.com/jairopaiva
 * GNU GPLv3
 * */

using BitcoinExprCracker.Generator;
using System;

namespace BitcoinExprCracker
{
    class Utils
    {

        public static ParsedPubKey ParseXYfromPub(byte[] pub, bool AssignULong = false)
        {
            byte[] X = new byte[32];
            byte[] Y = new byte[32];
            ParsedPubKey result = new ParsedPubKey();

            for (int i = 1; i < 33; i++)
            {
                X[i - 1] = pub[i];
            }

            for (int i = 0; i < 32; i++)
            {
                Y[i] = pub[i + 33];
            }

            result.X = X;
            result.Y = Y;

            if (AssignULong)
            {
                result.xU = Utils.ByteArrayToULong32Bits(X);
                result.yU = Utils.ByteArrayToULong32Bits(Y);
            }

            result.PublicKey = pub;

            return result;
        }

        public static void Resize(ref byte[] data, int lenght)
        {
            byte[] newdata = new byte[lenght];

            int e = data.Length - 1;
            int i = lenght - 1;
            while (e != -1)
            {
                if (i != -1)
                {
                    newdata[i] = data[e];
                    i--;
                    e--;
                }
            }
            data = newdata;

        }

        public static ulong[] ByteArrayToULong32Bits(byte[] array)
        {
            int total = array.Length / 4;
            ulong[] result = new ulong[total];

            for (int i = 0; i < total; i++)
            {
                byte[] arr = new byte[4];
                Buffer.BlockCopy(array, (i * 4), arr, 0, arr.Length);
                Array.Reverse(arr);
                result[i] = BitConverter.ToUInt32(arr, 0);
            }
            return result;
        }

        public static byte[] BigInt2Key(BigInteger privatekey)
        {
            byte[] prv = privatekey.getBytes();
            if (prv.Length != 32)
                Resize(ref prv, 32);
            return prv;
        }

        public static void ParseXYfromPub(byte[] pub, out byte[] x, out byte[] y)
        {
            byte[] X = new byte[32];
            byte[] Y = new byte[32];

            for (int i = 1; i < 33; i++)
            {
                X[i - 1] = pub[i];
            }

            for (int i = 0; i < 32; i++)
            {
                Y[i] = pub[i + 33];
            }

            x = X;
            y = Y;
        }

        public static int GetPubUIndex(int prvIndex)
        {
            return (int)(prvIndex / 4);
        }

    }
}

/* Criado por Jairo Paiva
 * https://github.com/jairopaiva
 * GNU GPLv3
 * */
