library(irr)


xassetCountrySector <- read.table( "Raters_CSV.tsv", sep="\t", header=TRUE)
dfa <- xassetCountrySector[,c(1,2)]

print(kappa2(dfa)$value)