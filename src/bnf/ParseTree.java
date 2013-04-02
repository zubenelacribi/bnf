package bnf;

import bnf.BnfDefParser.Tree;

public class ParseTree {

	public Tree tree;
	public String filename;
	
	public ParseTree(String filename, Tree tree) {
		this.filename = filename;
		this.tree = tree;
	}
	
	@Override
	public String toString() {
		return filename + '\n' + tree;
	}
	
}
