import os
import nltk
from nltk.corpus import conll2000, brown
from nltk.corpus.reader.tagged import TaggedCorpusReader
from nltk.stem import WordNetLemmatizer

lemmatizer = WordNetLemmatizer()

NLTK_PATH = '/mnt/DATA/data/nltk'
nltk.data.path.append(NLTK_PATH)

def get_pos(value):
    if value is None:
        return None
    return value.replace('-TL', '').replace('-HL', '')

def convert_corpora(corp):
    return '\n'.join(' '.join(['{}_{}'.format(x[0], get_pos(x[1])) for x in sent])
                     for sent in corp.tagged_sents())

with open("brown.txt", "w") as cf:
    cf.write(convert_corpora(brown))
with open("conll2000.txt", "w") as cf:
    cf.write(convert_corpora(conll2000))
# problem reports corpora
corpus_prob = TaggedCorpusReader(root=os.path.join(NLTK_PATH, 'corpora', 'problem_reports'),
                                 fileids=['apache', 'eclipse', 'firefox', 'linux', 'openoffice'],
                                 encoding='cp1252')
with open("problem_reports.txt", "w") as cf:
    cf.write(convert_corpora(corpus_prob))

# Change verbs to infinitive forms and store outputs as new corporas

def changed_infinitive(sent):
    return [(lemmatizer.lemmatize(pair[0], pos='v'), 'VB') if pair[1] in ['VBZ', 'VBD', 'VBG', 'VBN', 'VBP', 'VBZ']
            else pair for pair in sent]

def convert_modified_corpora(corp):
    return '\n'.join(' '.join(['{}_{}'.format(x[0], get_pos(x[1])) for x in changed_infinitive(sent)])
                     for sent in corp.tagged_sents())

with open("brown_modified.txt", "w") as cf:
    cf.write(convert_modified_corpora(brown))
with open("conll2000_modified.txt", "w") as cf:
    cf.write(convert_modified_corpora(conll2000))
with open("problem_reports_modified.txt", "w") as cf:
    cf.write(convert_modified_corpora(corpus_prob))