# OpenNLP Models Directory

This directory should contain the Apache OpenNLP language models required for natural language processing.

## Required Model Files:
1. **en-sent.bin** - English sentence detector model
2. **en-token.bin** - English tokenizer model  
3. **en-pos-maxent.bin** - English POS (Part-of-Speech) tagger model
4. **en-ner-person.bin** - English named entity recognition for persons
5. **en-ner-location.bin** - English named entity recognition for locations

## Download Instructions:
Download these models from: http://opennlp.sourceforge.net/models-1.5/

## Alternative:
The NLPProcessor has fallback mechanisms and will work with basic functionality even without these models.