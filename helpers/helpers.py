import sys
import math

# calculate expected jaccard from per-base error rate and kmer size
def G(eps, k):
    denom = 2 * math.exp(eps * k) - 1
    return 1.0 / denom

if __name__ == '__main__':
    eps  = float(sys.argv[1])
    print(G(eps, 16))
