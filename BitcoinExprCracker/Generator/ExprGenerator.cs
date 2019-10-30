/* Criado por Jairo Paiva
 * https://github.com/jairopaiva
 * GNU GPLv3
 * */

using System;
using System.Collections.Generic;
using System.IO;

namespace BitcoinExprCracker.Generator
{
    internal class ExprGenerator
    {
        /// <summary>
        ///
        /// </summary>
        /// <param name="targetX">X[i]</param>
        /// <param name="targetY">Y[i]</param>
        /// <param name="expression"></param>
        /// <returns></returns>

        public static int Generate(ref ulong Hash, ref ulong Seed, ref ulong limitDeOperações, ref byte[] X, ref byte[] Y, out string expression)
        {
            string stringOperation = string.Empty;
            List<Operação> Operações = new List<Operação>();

            ulong elementsAdded = 0;
            ulong insideTry = 0;

            for (ulong operações = 0; operações < limitDeOperações; operações++)
            {
                Operação op = new Operação();
                char Operador = GetOperand(true, Seed, ref insideTry, ref elementsAdded);

                stringOperation += "(";

                op.A = GenerateVariável(Seed, ref insideTry, ref elementsAdded, ref X, ref Y);

                if (Operador == Operands[6]) // ~ NOT
                {
                    op.A.Value = (byte)((~op.A.Value) & 255);

                    stringOperation += Operador + VariáveltoExpression(op.A) + " ";
                    Operador = GetOperand(false, Seed, ref insideTry, ref elementsAdded);
                }
                else
                    stringOperation += VariáveltoExpression(op.A) + " ";

                op.B = GenerateVariável(Seed, ref insideTry, ref elementsAdded, ref X, ref Y);

                stringOperation += Operador + " " + VariáveltoExpression(op.B) + " ";

                // op.Resultado = (byte)(operators[Operador](op.A.Value, op.B.Value) % 256);
                op.Resultado = (byte)(CalcularOperação(Operador, new int[] { op.A.Value, op.B.Value }) % 256);
                op.Operador = Operador;
                Operações.Add(op);

                stringOperation += ") ";

                if (operações + 1 < limitDeOperações)
                {
                    Operações.Add(new Operação() { Operador = GetOperand(false, Seed, ref insideTry, ref elementsAdded) });
                    stringOperation += Operações[Operações.Count - 1].Operador + " ";
                }
            }

            // Agora calcular o resultado da expressão

            int resultado = CalcularResultadoOperações(Operações);
            expression = stringOperation;

            return resultado;
        }

        public static int Calculate(byte[] pubkey, ref string expression)
        {
            byte[] compareX = new byte[32];
            byte[] compareY = new byte[32];
            Utils.ParseXYfromPub(pubkey, out compareX, out compareY);

            return Calculate(compareX, compareY, ref expression);
        }

        public static int Calculate(ParsedPubKey pubkey, ref string expression)
        {
            return Calculate(pubkey.X, pubkey.Y, ref expression);
        }

        public static int Calculate(byte[] compareX, byte[] compareY, ref string expression)
        {
            List<Operação> Operações = new List<Operação>();
            // Vars = new List<Variável>();

            StringReader Reader = new StringReader(expression.Replace(" ", ""));
            bool B = false;

            char c = (char)Reader.Peek();

            while (Reader.Peek() != -1)
            {
                c = (char)Reader.Peek();

                if (c == '(')
                {
                    Operação op = new Operação(); //    (~X[10] ^ ~Y[21] ) ^ (~Y[9] & ~X[24] )
                    Reader.Read();

                VOLTA:

                    c = (char)Reader.Peek();

                    if (c == '~')
                    {
                        Reader.Read();

                        char Letra = (char)Reader.Peek();
                        Reader.Read(); // [
                        Reader.Read(); // index

                        string numero = string.Empty;
                        int index = 0;

                        while (char.IsNumber((char)Reader.Peek()))
                        {
                            numero += (char)Reader.Peek();
                            Reader.Read();
                        }
                        index = int.Parse(numero);

                        Reader.Read(); // ]

                        if (!B)
                        {
                            op.A = GetVariável(ref compareX, ref compareY, Letra, index, true);
                            //  if(Vars != null)
                            //  Vars.Add(op.A);
                            B = true;
                            goto VOLTA;
                        }
                        else    // A já foi prenchido, agora B
                        {
                            op.B = GetVariável(ref compareX, ref compareY, Letra, index, true);
                            //  if (Vars != null)
                            //     Vars.Add(op.B);
                        }

                        Reader.Read(); // )

                        if (B)
                        {
                            Operações.Add(op);
                            // op.Resultado = (byte)(operators[op.Operador](op.A.Value, op.B.Value) % 256);
                            op.Resultado = (byte)(CalcularOperação(op.Operador, new int[] { op.A.Value, op.B.Value }) % 256);
                            B = false;
                            continue;
                        }
                    }
                    else if (IsOperand(c))
                    {
                        op.Operador = c;
                        Reader.Read();
                        goto VOLTA;
                    }
                    else
                    {
                        char Letra = (char)Reader.Peek();
                        Reader.Read(); // [
                        Reader.Read(); // index

                        string numero = string.Empty;
                        int index = 0;

                        while (char.IsNumber((char)Reader.Peek()))
                        {
                            numero += (char)Reader.Peek();
                            Reader.Read();
                        }

                        index = int.Parse(numero);

                        Reader.Read(); // ]

                        if (!B)
                        {
                            op.A = GetVariável(ref compareX, ref compareY, Letra, index);
                            //  if (Vars != null)
                            //      Vars.Add(op.A);
                            B = true;
                            goto VOLTA; ;
                        }
                        else // A já foi prenchido, agora B
                        {
                            op.B = GetVariável(ref compareX, ref compareY, Letra, index);
                            //  if (Vars != null)
                            //     Vars.Add(op.B);
                        }

                        Reader.Read(); // )

                        if (B)
                        {
                            //op.Resultado = (byte)(operators[op.Operador](op.A.Value, op.B.Value) % 256);
                            op.Resultado = (byte)(CalcularOperação(op.Operador, new int[] { op.A.Value, op.B.Value }) % 256);
                            Operações.Add(op);
                            B = false;
                            continue;
                        }
                    }
                }
                else if (IsOperand(c))
                {
                    Operações.Add(new Operação() { Operador = c });
                    Reader.Read(); // (
                }
            }

            return CalcularResultadoOperações(Operações);
        }

        private static int CalcularResultadoOperações(List<Operação> Operações)
        {
        // Agora calcular o resultado da expressão

        aindaHáOperações:
            List<Operação> temp = new List<Operação>();

            for (int i = 0; i < Operações.Count; i++)
            {
                Operação a = Operações[i];
                i++;
                char operador = Operações[i].Operador;
                i++;
                Operação b = Operações[i];

                Operação resultado = new Operação();
                // resultado.Resultado = (byte)(operators[operador](a.Resultado, b.Resultado) % 256);
                resultado.Resultado = (byte)(CalcularOperação(operador, new int[] { a.Resultado, b.Resultado }) % 256);
                temp.Add(resultado);

                if (i == Operações.Count - 1)
                    break;
                i++;
                temp.Add(Operações[i]);
            }

            if (temp.Count != 1)
            {
                Operações = temp;
                goto aindaHáOperações;
            }

            return (byte)temp[0].Resultado;
        }

        private static Variável GetVariável(ref byte[] x, ref byte[] y, char x_y, int index, bool NOT = false)
        {
            Variável v = new Variável();
            if (x_y == 'X')
            {
                if (!NOT)
                    v.Value = x[index];
                else
                    v.Value = (byte)((~x[index]) & 255);
            }
            else // 'Y'
            {
                if (!NOT)
                    v.Value = y[index];
                else
                    v.Value = (byte)((~y[index]) & 255);
            }

            v.X_Y = x_y;
            v.Index = index;
            return v;
        }

        private static readonly char[] Operands = { '+', '-', '*', '%', '&', '|', '~', '^' };

        private static bool IsOperand(char c)
        {
            if (Operands[0] == c || Operands[1] == c || Operands[2] == c || Operands[3] == c || Operands[4] == c ||
              Operands[5] == c || Operands[6] == c || Operands[7] == c)
                return true;

            return false;
        }

        private static int CalcularOperação(char op, int[] values)
        {
            switch (op)
            {
                case '+':
                    return values[0] + values[1];

                case '-':
                    return values[0] - values[1];

                case '*':
                    return values[0] * values[1];

                case '%':
                    if (values[0] == 0)
                    {
                        if (values[1] != 0)
                            return values[1];
                        else
                            return 0;
                    }
                    else if (values[1] == 0)
                    {
                        return 0;
                    }
                    else
                        return values[0] % values[1];

                case '&':
                    return values[0] & values[1];

                case '|':
                    return values[0] | values[1];

                case '~':
                    return ~values[0];

                case '^':
                    return values[0] ^ values[1];

                default:
                    throw new Exception();
            }
        }

        private static string VariáveltoExpression(Variável v)
        {
            string ret = v.X_Y + "[" + v.Index + "]";
            return ret;
        }

        public static ulong Hash(byte[] bytes)
        {
            ulong x = 1;
            for (int i = 0; i < bytes.Length; i++)
            {
                x ^= bytes[i];
                x ^= x * (x >> 3);
                x += (x >> 3) ^ (x << 1);
            }
            return x;
        }

        private static Variável GenerateVariável(ulong seed, ref ulong insideTry, ref ulong elementsAdded, ref byte[] X, ref byte[] Y)
        {
            Variável variável = new Variável();
            byte[] indexSeed = BitConverter.GetBytes(seed + insideTry + elementsAdded);
            Array.Reverse(indexSeed);

            int Index = (int)(Hash(indexSeed) % 32);
            ulong X_Y = ((ulong)Index + insideTry) % 2;

            if (X_Y == 0)
            {
                variável.Index = Index;
                variável.Value = X[Index];
                variável.X_Y = 'X';
            }
            else  // X_Y == 1
            {
                variável.Index = Index;
                variável.Value = Y[Index];
                variável.X_Y = 'Y';
            }

            elementsAdded++;
            return variável;
        }

        public static char GetOperand(bool podeNot, ulong seed, ref ulong insideTry, ref ulong elementsAdded)
        {
        volta:
            byte[] indexSeed = BitConverter.GetBytes(seed + insideTry + elementsAdded);
            Array.Reverse(indexSeed);
            int result = (int)(Hash(indexSeed) % 8);

            if (!podeNot && result == 6)
            {
                insideTry += 1;
                goto volta;
            }
            elementsAdded++;

            return Operands[result];
        }
    }
}

/* Criado por Jairo Paiva
 * https://github.com/jairopaiva
 * GNU GPLv3
 * */
