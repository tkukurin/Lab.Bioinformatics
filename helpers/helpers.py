import sys
import math


# E(J)
# calculate expected jaccard from per-base error rate and kmer size
def G(eps, k):
  denom = 2 * math.exp(eps * k) - 1
  return 1.0 / denom


# E(e)
# calculate expected per-base error rate from jaccard and kmer size
def F(J, k):
  m1 = - 1.0 / k
  m2 = (2.0 * J) / (1 + J)
  return m1 * math.log(m2)


if __name__ == '__main__':
  param = float(sys.argv[1])
  kmer = 16 if len(sys.argv) == 2 else int(sys.argv[2])

  print('E(eps) = F({}, {}) ='.format(param, kmer), F(param, kmer))
  print('E(Jac) = G({}, {}) ='.format(param, kmer), G(param, kmer))

