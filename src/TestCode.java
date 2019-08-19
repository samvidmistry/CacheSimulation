class TestCode {
    public static void main(String[] args) {
        double[] y = new double[512];
        double[][] A = new double[512][512], X = new double[512][512];
        for (int k = 0; k < 512; k++) {
            for (int j = 0; j < 512; j++) {
                for (int i = 0; i < 512; i++) {
                    y[i] = y[i] + A[i][j] * X[k][j];
                }
            }

            System.out.printf("Loop k = %d\n", k);
        }
    }
}