// Fill in the additionalInfo array.
additionalInfo = new String[2];
additionalInfo[0] = "pca_vectors";        
StringBuilder sb = new StringBuilder();
int rows = svdv.getRowDimension();
for (int i = 0; i < rows; i++) {
    double [] row = svdv.getRow(i);
    sb.append(Arrays.toString(row));
    sb.append("\n");
}
// Remove brackets
String temp = sb.toString();
temp = temp.replace("[", "");
temp = temp.replace("]", "");
additionalInfo[1] = temp;