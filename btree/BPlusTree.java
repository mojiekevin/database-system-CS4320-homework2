import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

/**
 * BPlusTree Class Assumptions: 1. No duplicate keys inserted 2. Order D:
 * D<=number of keys in a node <=2*D 3. All keys are non-negative
 * TODO: Rename to BPlusTree
 */
public class BPlusTree<K extends Comparable<K>, T> {

	public Node<K,T> root;
	public static final int D = 2;

	/**
	 * TODO Search the value for a specific key
	 * @param key
	 * @return value
	 */
	public T search(K key) {
		if (key == null)
			return null;
		return searchNode(root, key);
	}
	
	/**
	 * find the matched value with k in bplus tree root
	 * @param root root of the bplus tree or subtree
	 * @param key key we need to search
	 * @return the matched value with k
	 */
	private T searchNode(Node<K, T> curr, K key) {
		if (curr.isLeafNode) { // if it is leaf node
			List<K> keys = curr.keys;
			List<T> values = ((LeafNode<K, T>) curr).values;
			if (keys == null || values == null) // if list is null, cannot continue searching. return null
				return null;
			int location = findKeyLocation(keys, key);
			if (location == -1) { // if such key cannot find
				return null;
			}
			else {
				return values.get(location);
			}
		}
		else {
			List<K> keys = curr.keys;
			List<Node<K, T>> children = ((IndexNode<K, T>) curr).children;
			if (keys.size() + 1 != children.size())
				return null; // invalid BPlus tree, return null
			int m = findKeyRange(keys, key);
			return searchNode(children.get(m), key);
		}
	}

	/**
	 * TODO Insert a key/value pair into the BPlusTree
	 * 
	 * @param key
	 * @param value
	 */
	public void insert(K key, T value) {
		if (key == null || value == null) { // invalid input
			return;
		}
		if (root == null) { // if current root is null, create a new one which should be leaf node. add this <key, value> pair
			root = new LeafNode<K, T>(key, value);
		}
		else { // if current root is not null, using dfs to insert such <key, value> pair
			Entry<K, Node<K, T>> entry = insert(root, key, value);
			if (entry != null) { // if entry is not null, it means current root should be split
				/** everything is done in dfs, except creating a new root node */
				IndexNode<K, T> newRoot = new IndexNode<K, T>(entry.getKey(), root, entry.getValue());
				root = newRoot;
			}
		}
	}
	
	/**
	 * insert the <key, value> into the BPlus tree with the root
	 * @param root root of BPlus tree
	 * @param key
	 * @param value
	 * @return the entry if split happens. or null if nothing happens
	 */
	private Entry<K, Node<K,T>> insert(Node<K, T> root, K key, T value) {
		if (root == null || key == null || value == null)
			return null;
		/** if this node is leaf node, insert this value and detect whether needs to be split */
		if (root.isLeafNode) {
			((LeafNode<K, T>) root).insertSorted(key, value); // insert this <key, value> pair
			if (root.keys.size() > 2 * D) { // if size is bigger than 2 * D, split it
				return splitLeafNode(((LeafNode<K, T>) root), root.keys.get(D));
			}
			else { // if not, we don't need to split
				return null;
			}
		}
		/** if this node is index node, keep searching for the key using dfs with bottom-up.
		 * if return is not null, insert key into current node and detect whether needs to be split 
		 */
		else {
			List<K> keys = root.keys;
			List<Node<K, T>> children = ((IndexNode<K, T>) root).children;
			int m = findKeyRange(keys, key);
			Entry<K, Node<K, T>> temp = insert(children.get(m), key, value); // bottom up, get the inserted result below this level
			if (temp != null) { // if temp is not null, split below this level has happened. insert this new element into the node of this level
				keys.add(m, temp.getKey());
				children.add(m + 1, temp.getValue());
				if (root.keys.size() > 2 * D) { // if split needs to happen again
					K splitKey = root.keys.get(D); // find the split key
					return splitIndexNode((IndexNode<K, T>) root, splitKey); // split this node, return it to the upper level
				}
				else { // if not, return null
					return null;
				}
			}
			else // if below this level, no split happens, do nothing, return null to the upper level
				return null;
		}
	}

	/**
	 * Split a leaf node and return the new right node and the splitting
	 * key as an Entry<slitingKey, RightNode>
	 * 
	 * @param leaf, any other relevant data
	 * @return the key/node pair as an Entry
	 */
	public Entry<K, Node<K,T>> splitLeafNode(LeafNode<K,T> leaf, K splitKey) {
		if (leaf == null || splitKey == null) // invalid parameters
			return null;
		List<K> keys = leaf.keys;
		List<T> values = leaf.values;
		if (keys == null || values == null || keys.size() != values.size()) // invalid bplus tree
			return null;
		int i = findKeyLocation(keys, splitKey);
		if (i == -1) // can't find such key in this node
			return null;
		/** add right elements of leaf node([splitKey, ...) into this new created node which should be the right node */
		LeafNode<K, T> rightNode = new LeafNode<K, T>(keys.get(i), values.get(i));
		for (int j = i + 1; j < keys.size(); j++) {
			rightNode.insertSorted(keys.get(j), values.get(j));
		}
		/** delete these elements in the original leaf node */
		while (i < keys.size()) {
			keys.remove(i);
			values.remove(i);
		}
		/** connect leaf node and new created right node */
		rightNode.nextLeaf = leaf.nextLeaf;
		rightNode.previousLeaf = leaf;
		leaf.nextLeaf = rightNode;
		Entry<K,Node<K, T>> entry = new AbstractMap.SimpleEntry<K,Node<K, T>>(splitKey, rightNode);
		return entry;
	}

	/**
	 * split an indexNode and return the new right node and the splitting
	 * key as an Entry<slitingKey, RightNode>
	 * 
	 * @param index, any other relevant data
	 * @return new key/node pair as an Entry
	 */
	public Entry<K, Node<K,T>> splitIndexNode(IndexNode<K,T> index, K splitKey) {
		if (index == null || splitKey == null) // invalid parameters
			return null;
		List<K> keys = index.keys;
		List<Node<K, T>> children = ((IndexNode<K, T>) index).children;
		int i = findKeyLocation(keys, splitKey);
		if (i == -1) // can't find such key in this node
			return null;
		/** form the right node */
		int j = i + 1; // get the element index on the right of splitKey
		/** add right elements of leaf node([splitKey, ...) into this new created node which should be the right node */
		List<K> rightKeys = new ArrayList<K>();
		List<Node<K, T>> rightChildren = new ArrayList<Node<K, T>>();
		for (; j < keys.size(); j++) {
			rightKeys.add(keys.get(j));
			rightChildren.add(children.get(j));
		}
		rightChildren.add(children.get(keys.size()));
		IndexNode<K, T> rightNode = new IndexNode<K, T>(rightKeys, rightChildren);
		/** remove these elements from original node */
		while (i < keys.size()) {
			keys.remove(i);
			children.remove(i + 1);
		}
		Entry<K,Node<K, T>> entry = new AbstractMap.SimpleEntry<K,Node<K, T>>(splitKey, rightNode);
		return entry;
	}

	/**
	 * TODO Delete a key/value pair from this B+Tree
	 * 
	 * @param key
	 */
	public void delete(K key) {
		if (key == null) // invalid input
			return;
		if (root.isLeafNode) { // if root itself is leaf node
			List<K> keys = root.keys;
			int i = findKeyLocation(keys, key);
			if (i == -1) // can't find such key in this node
				return;
			else {
				keys.remove(i);
			}
		}
		else {
			List<K> keys = root.keys;
			List<Node<K, T>> children = ((IndexNode<K, T>) root).children;
			int m = findKeyRange(keys, key);
			int ret = delete(children.get(m), key, (IndexNode<K, T>) root, m);
			if (ret != -1) { // merge happens next level
				keys.remove(ret);
				children.remove(ret); 
				if (keys.size() == 0) { // if root is empty, delete it, let it points to the current child node
					root = children.get(0);
				}
				return;
			}
		}
	}
	
	/**
	 * delete node using dfs, detect whether merge or redistribute is needed during dfs
	 * @param curr current node
	 * @param key searched key
	 * @param parent parent node of current node
	 * @param index the index of current node
	 * @return index of node which should be deleted in parent node. -1 if no merge happens
	 */
	private int delete(Node<K, T> curr, K key, IndexNode<K, T> parent, int index) {
		if (curr == null || key == null) // invalid input
			return -1;
		if (curr.isLeafNode) { // if it is LeafNode
			List<K> keys = curr.keys;
			int i = findKeyLocation(keys, key);
			if (i == -1) // can't find such key in this node
				return -1;
			keys.remove(i);
			((LeafNode<K, T>) curr).values.remove(i);
			if (index == 0 && curr.isUnderflowed()) { // if smaller node is on the left side, this time index = 0, so bigger node should be the nextLeaf
				return handleLeafNodeUnderflow(((LeafNode<K, T>) curr), ((LeafNode<K, T>) curr).nextLeaf, parent);
			}
			else if (curr.isUnderflowed()) { // if smaller node is on the right side, so bigger node should be the previousLeaf
				return handleLeafNodeUnderflow(((LeafNode<K, T>) curr), ((LeafNode<K, T>) curr).previousLeaf, parent);
			}
			return -1; // current node is not underflowed
		}
		else { // if it is IndexNode
			List<K> keys = curr.keys;
			List<Node<K, T>> children = ((IndexNode<K, T>) curr).children;
			int m = findKeyRange(keys, key);
			int ret = delete(children.get(m), key, ((IndexNode<K, T>) curr), m);
			if (ret != -1) { // merge happens on the below level
				keys.remove(ret); // delete this key
				children.remove(ret); // remove left child of this key
				if (curr.isUnderflowed()) { // if current node is underflowed
//					if (curr.keys.size() == 0) { // if current index node is empty, change it as LeafNode
//						curr = children.get(1);
//						return -1;
//					}
					if (index == 0) { // if smaller node is on the left side, this time index = 0, so bigger node should be at index + 1
						return handleIndexNodeUnderflow((IndexNode<K, T>) curr, (IndexNode<K, T>) parent.children.get(index + 1), parent);
					}
					else { // if smaller node is on the right side, so bigger node should be at index - 1
						return handleIndexNodeUnderflow((IndexNode<K, T>) curr, (IndexNode<K, T>) parent.children.get(index - 1), parent);
					}
				}
				else
					return -1;
			}
			else
				return -1;
		}
	}

	/**
	 * Handle LeafNode Underflow (merge or redistribution)
	 * 
	 * @param left
	 *            : the smaller node
	 * @param right
	 *            : the bigger node
	 * @param parent
	 *            : their parent index node
	 * @return the splitkey position in parent if merged so that parent can
	 *         delete the splitkey later on. -1 otherwise
	 */
	public int handleLeafNodeUnderflow(LeafNode<K,T> smaller, LeafNode<K,T> bigger, IndexNode<K,T> parent) {
		if (smaller == null || bigger == null || parent == null || smaller.keys.size() > bigger.keys.size()) // invalid input
			return -1;
		if (smaller.keys.size() >= D) { // no need to redistribute or merge
			return -1;
		}
		else if (bigger.keys.size() >= D + 1) { // redistribute
			/** find the key of the parent node with these left and right child node*/
			int location = findParentLocation(smaller, bigger, parent);
			/** move one element from bigger to smaller node */
			moveOneFromBigger(smaller, bigger, parent, location);
			return -1;
		}
		else { // merge
			/** find the key of the parent node with these left and right child node*/
			int location = findParentLocation(smaller, bigger, parent);
			/** Invariant:
			 *  move all elements from left to right even though smaller node is on the left side
			 */
			if (smaller.keys.get(0).compareTo(bigger.keys.get(0)) >= 0) { // smaller node is on the right side
				List<K> keys = bigger.keys;
				List<T> values = bigger.values;
				for (int j = keys.size() - 1; j >= 0; j--) {
					smaller.keys.add(0, keys.get(j));
					smaller.values.add(0, values.get(j));
				}
				smaller.previousLeaf = bigger.previousLeaf;
			}
			else { // smaller node is on the left side, index should be 0
				List<K> keys = smaller.keys;
				List<T> values = smaller.values;
				for (int j = smaller.keys.size() - 1; j >= 0; j--) {
					bigger.keys.add(0, keys.get(j));
					bigger.values.add(0, values.get(j));
				}
				bigger.previousLeaf = smaller.previousLeaf;
			}
			return location;
		}
	}

	/**
	 * Handle IndexNode Underflow (merge or redistribution)
	 * 
	 * @param smallerIndex smaller node which is underflowed
	 * @param biggerIndex  bigger node which is not underflowed but want to help smaller node
	 * @param parent
	 *            : their parent index node
	 * @return the splitkey position in parent if merged so that parent can
	 *         delete the splitkey later on. -1 otherwise
	 */
	public int handleIndexNodeUnderflow(IndexNode<K,T> smallerIndex, IndexNode<K,T> biggerIndex, IndexNode<K,T> parent) {
		if (smallerIndex == null || biggerIndex == null || parent == null || smallerIndex.keys.size() > biggerIndex.keys.size()) // invalid input
			return -1;
		if (smallerIndex.keys.size() >= D) { // no need to redistribute or merge
			return -1;
		}
		else if (biggerIndex.keys.size() >= D + 1) { // redistribute
			/** find the key of the parent node with these left and right child node*/
			int location = findParentLocation(smallerIndex, biggerIndex, parent);
			/** move one element from bigger to smaller node */
			moveOneFromBigger(smallerIndex, biggerIndex, parent, location);
			/** redistribute is special in IndexNode, we need to pull the parent key down and push the bigger key up */
			if (smallerIndex.keys.get(0).compareTo(biggerIndex.keys.get(0)) >= 0) { // smaller node is on the right side
				K rmKey = parent.keys.remove(location);
				parent.keys.add(location, smallerIndex.keys.get(0));
				smallerIndex.keys.remove(0);
				smallerIndex.keys.add(0, rmKey);
			}
			else {
				K rmKey = parent.keys.remove(location);
				parent.keys.add(location, smallerIndex.keys.get(smallerIndex.keys.size() - 1));
				smallerIndex.keys.remove(smallerIndex.keys.size() - 1);
				smallerIndex.keys.add(rmKey);
			}
			return -1;
		}
		else { // merge
			int location = findParentLocation(smallerIndex, biggerIndex, parent);
			//parent.children.remove(location + 1); // remove right child node
			/** move all elements from left to right */
			if (smallerIndex.keys.get(0).compareTo(biggerIndex.keys.get(0)) >= 0) { // smaller node is on the right side
				smallerIndex.keys.add(0, parent.keys.get(location)); // move parent node with key k to right node
				/** move all left keys and children to right node */
				for (int i = biggerIndex.keys.size() - 1; i >= 0; i--) {
					smallerIndex.keys.add(0, biggerIndex.keys.get(i));
					smallerIndex.children.add(0, biggerIndex.children.get(i + 1));
				}
				smallerIndex.children.add(0, biggerIndex.children.get(0));
			}
			else { // smaller node is on the left side, which index should be 0
				biggerIndex.keys.add(0, parent.keys.get(location)); // move parent node with key k to right node
				/** move all left keys and children to right node */
				for (int i = smallerIndex.keys.size() - 1; i>= 0; i--) {
					biggerIndex.keys.add(0, smallerIndex.keys.get(i));
					biggerIndex.children.add(0, smallerIndex.children.get(i + 1));
				}
				biggerIndex.children.add(0, smallerIndex.children.get(0));
			}
			return location;
		}
	}
	
	/**
	 * deal with redistribute
	 * move one element from the bigger node to the smaller node
	 * @param smaller smaller node which is underflowed
	 * @param bigger bigger node which is not underflowed but want to help smaller node
	 * @param parent parent node of these two nodes
	 * @param location the index of key that parent node should delete if merge happens
	 */
	private void moveOneFromBigger(Node<K, T> smaller, Node<K, T> bigger, Node<K, T> parent, int location) {
		if (smaller.keys.get(0).compareTo(bigger.keys.get(0)) > 0) { // smaller node is on the right side
			int size = bigger.keys.size();
			/** add one element from bigger to smaller, and remove it from bigger */
			smaller.keys.add(0, bigger.keys.get(bigger.keys.size() - 1));
			bigger.keys.remove(size - 1);
			if (smaller.isLeafNode) {
				((LeafNode<K, T>) smaller).values.add(0, ((LeafNode<K, T>) bigger).values.get(size - 1));
				((LeafNode<K, T>) bigger).values.remove(size - 1);
			}
			else {
				((IndexNode<K, T>) smaller).children.add(0, ((IndexNode<K, T>) bigger).children.get(size));
				((IndexNode<K, T>) bigger).children.remove(size);  
			}
			/** change the key of same location in parent node */
			parent.keys.remove(location); // remove that key
			parent.keys.add(location, smaller.keys.get(0)); // add a new key at the same location
		}
		else { // smaller node is on the left side, where index should be 0
			/** add one element from bigger to smaller, and remove it from bigger */
			smaller.keys.add(bigger.keys.get(0));
			bigger.keys.remove(0);
			if (smaller.isLeafNode) {
				((LeafNode<K, T>) smaller).values.add(((LeafNode<K, T>) bigger).values.get(0));
				((LeafNode<K, T>) bigger).values.remove(0); 
			}
			else {
				((IndexNode<K, T>) smaller).children.add(((IndexNode<K, T>) bigger).children.get(0)); 
				((IndexNode<K, T>) bigger).children.remove(0);
			}
			/** change the key of same location in parent node */
			parent.keys.remove(location); // remove that key
			parent.keys.add(location, bigger.keys.get(0)); // add a new key at the same location
		}
	}

	/**
	 * find the location of the parent node which has left and right child nodes
	 * @param left left child node
	 * @param right right child node
	 * @param parent parent nodes group
	 * @return the location of the parent node which has left and right nodes
	 */
	private int findParentLocation(Node<K, T> left, Node<K, T> right, Node<K, T> parent) {
		if (left.keys.get(0).compareTo(right.keys.get(0)) < 0) { // left is smaller than right
			for (int i = 0; i < parent.keys.size(); i++) {
				if ((parent.keys.get(i).compareTo(left.keys.get(left.keys.size() - 1)) >= 0)
					&& (parent.keys.get(i).compareTo(right.keys.get(0)) <= 0)) {
					return i;
				}
			}
			return -1;
		}
		else {
			for (int i = 0; i < parent.keys.size(); i++) {
				if ((parent.keys.get(i).compareTo(right.keys.get(right.keys.size() - 1)) >= 0)
					&& (parent.keys.get(i).compareTo(left.keys.get(0)) <= 0)) {
					return i;
				}
			}
			return -1;
		}
	}
	
	/**
	 * return the location of key in list keys
	 * @param keys list of keys 
	 * @param key search the location of key
	 * @return the location of key, -1 if not found
	 */
	private int findKeyLocation(List<K> keys, K key) {
		int begin = 0;
		int end = keys.size() - 1;
		while (begin <= end) {
			int middle = begin + (end - begin) / 2;
			if (keys.get(middle).compareTo(key) > 0) {
				end = middle - 1;
			}
			else if (keys.get(middle).compareTo(key) < 0) {
				begin = middle + 1;
			}
			else {
				return middle;
			}
		}
		return -1;
	}
	
	/**
	 * find the region of child for the key in keys
	 * @param keys list of keys
	 * @param key the key we need to use to find the region of child
	 * @return the index of child depending on the region
	 */
	private int findKeyRange(List<K> keys, K key) {
		int begin = 0;
		int end = keys.size() - 1;
		if (key.compareTo(keys.get(end)) >= 0)
			return end + 1;
		while (begin < end) {
			int middle = begin + (end - begin) / 2;
			if (key.compareTo(keys.get(middle)) < 0) {
				end = middle;
			}
			else {
				begin = middle + 1;
			}
		}
		return begin;
	}
}