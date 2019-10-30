/* Criado por Jairo Paiva
 * https://github.com/jairopaiva
 * GNU GPLv3
 * */
namespace BitcoinExprCracker.Generator
{
    public struct Operação
    {
        public Variável A;
        public Variável B;
        public char Operador;
        public byte Resultado;
    }

    public struct  Variável
    {
        public byte  Value; // Valor
        public int Index; // Index em X ou Y
        public char X_Y; // Se pertence a x ou Y

        public override string ToString()
        {
            return  Value.ToString();
        }
    }

    public struct ParsedPubKey
    {
        public byte[] X;
        public byte[] Y;
        public ulong[] xU;
        public ulong[] yU;
        public byte[] PublicKey;
    }
}