/* Criado por Jairo Paiva
 * https://github.com/jairopaiva
 * GNU GPLv3
 * */

using NBitcoin;
using System;

namespace BitcoinExprCracker.Generator
{
    class GeneratorMethods
    {

        public static byte[] ConvertTryesToPrivateKey(byte[] publicKey, ulong[] tryes)
        {
            ulong UintMax = 4294967296;
            ParsedPubKey pubKey = Utils.ParseXYfromPub(publicKey, true);
            byte[] prv = new byte[32];

            for (int indexPrv = 0; indexPrv < 32; indexPrv++)
            {
                ulong targetX = pubKey.xU[Utils.GetPubUIndex(indexPrv)];
                ulong targetY = pubKey.yU[Utils.GetPubUIndex(indexPrv)];
                ulong Mult = targetX * targetY;
                ulong SeedSum = (ulong)((new BigInteger(pubKey.X) % UintMax).LongValue() + (new BigInteger(pubKey.Y) % UintMax).LongValue());

                ulong Try = tryes[indexPrv];

                ulong Hash = Try + (Mult * ((ulong)indexPrv + Try)) % UintMax;
                ulong limitDeOperações = (uint)Math.Pow(2d, (double)(Hash % 2) + 1d);
                ulong SeedC = Try + Mult;
                ulong Seed = ((Hash + SeedSum + SeedC) + Try) % UintMax;

                string Expr = string.Empty;
                byte result = (byte)ExprGenerator.Generate(ref Hash, ref Seed, ref limitDeOperações, ref pubKey.X, ref pubKey.Y, out Expr);

                prv[indexPrv] = result;
            }

            return prv;
        }

        public static ulong[] GetPubKeyCorrectTryes(Key key, int limitPrvIndex = 32, bool PrintCorrectExpr = false)
        {
            ulong UintMax = 4294967296;
            ParsedPubKey pubKey = Utils.ParseXYfromPub(key.PubKey.Decompress().ToBytes(), true);
            byte[] prv = key.ToBytes();
            ulong[] CorrectTryes = new ulong[32];

            for (int indexPrv = 0; indexPrv < limitPrvIndex; indexPrv++)
            {
                ulong targetX = pubKey.xU[Utils.GetPubUIndex(indexPrv)];
                ulong targetY = pubKey.yU[Utils.GetPubUIndex(indexPrv)];
                ulong Mult = targetX * targetY;
                ulong SeedSum = (ulong)((new BigInteger(pubKey.X) % UintMax).LongValue() + (new BigInteger(pubKey.Y) % UintMax).LongValue());

                for (ulong Try = 0; Try < ulong.MaxValue; Try++)
                {
                    ulong Hash = Try + (Mult * ((ulong)indexPrv + Try)) % UintMax;
                    ulong limitDeOperações = (uint)Math.Pow(2d, (double)(Hash % 2) + 1d);
                    ulong SeedC = Try + Mult;
                    ulong Seed = ((Hash + SeedSum + SeedC) + Try) % UintMax;

                    string Expr = string.Empty;
                    byte result = (byte)ExprGenerator.Generate(ref Hash, ref Seed, ref limitDeOperações, ref pubKey.X, ref pubKey.Y, out Expr);

                    if (prv[indexPrv] == result)
                    {
                        CorrectTryes[indexPrv] = Try;
                        if (PrintCorrectExpr)
                            Console.WriteLine("Correct expression for index "+indexPrv + " = " +Expr);
                        break;
                    }
                }
            }

            return CorrectTryes;
        }

        public static string TryesToString(ulong[] CorrectTryes)
        {
            string rett = "";
            for (int i = 0; i < CorrectTryes.Length; i++)
            {
                rett += (CorrectTryes[i] + ", ");
            }
            return rett;
        }
        
    }
}

/* Criado por Jairo Paiva
 * https://github.com/jairopaiva
 * GNU GPLv3
 * */
