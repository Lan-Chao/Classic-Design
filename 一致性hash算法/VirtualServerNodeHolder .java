
//在节点持有容器内，通过ArrayList数据结构来存储虚拟节点；并且持有一个Set集合，用来存储物理节点的节点名，防止出现重复添加的情况发生。
//同时，在增加物理节点的时候，会将该物理节点映射为5个虚拟节点，并以128bit的hash结果顺序排序。

  /**
  * 虚拟节点容器
  */
  
@Slf4j
public class VirtualServerNodeHolder {
     private final static String VIRTUAL_SPLIT = "&&VN";
     private final static int VIRTUAL_NUM = 5;
     private final int seed;
     /**
     * 物理节点名称集合
     * 用于判重，防止相同节点重复加入
     */
     private final Set<String> names;
     private final List<VirtualServerNode> nodes;
     public VirtualServerNodeHolder(int seed) {
         this.seed = seed;
         this.names = new HashSet<>();
         this.nodes = new ArrayList<>();
     }
     public VirtualServerNodeHolder(int seed, List<VirtualServerNode> nodes) {
         this.seed = seed;
         this.names = nodes.stream().map(VirtualServerNode::getServerNode).collect(Collectors.toSet());
         this.nodes = nodes;
     }
     public synchronized void addPhyNode(String serverName) {
         if (serverName == null || nodes.isEmpty()) {
             return;
         }
         // hook 判断是否存在相同名称节点
         if (names.contains(serverName)) {
             log.error("已经存在相同名称节点 {}，忽略该节点", serverName);
             return;
         }
         names.add(serverName);
         for (int i = 0; i < VIRTUAL_NUM; i++) {
             // 设定虚拟节点的名字格式为：serverName + "&&VN" + i，方便从虚拟节点得到物理节点
             String virtualServerNodeName = serverName + VIRTUAL_SPLIT + i;
             MurmurHash3.HashValue hash = getHash(virtualServerNodeName);
             VirtualServerNode vsNode = new VirtualServerNode(virtualServerNodeName, hash);
             nodes.add(vsNode);
         }
         //将虚拟节点列表进行排序
         Collections.sort(nodes);
     }
     public synchronized void delPhyNode(String serverName) {
         if (serverName == null || nodes.isEmpty()) {
            return;
         }
         for (int i = 0; i < VIRTUAL_NUM; i++) {
             VirtualServerNode node = nodes.get(i);
             if (node.getServerNode().contains(serverName)) {
                 nodes.remove(node);
                 i--;
             }
         }
         // 完成所有节点删除后，删除名字
         names.remove(serverName);
         }
      @Nullable
      public synchronized VirtualServerNode getServerNode(String key) {
         if (nodes.isEmpty()) {
            return null;
         }
         //得到key的hash值
         MurmurHash3.HashValue hash = getHash(key);
         for (VirtualServerNode node : nodes) {
             if (VirtualServerNode.compareTo(node.getNodeHash(), hash) == 1) {
                return node;
             }
         }
         //如果没有找到，则说明此key的hash值比所有服务器节点的hash值都大，因此返回最小hash值的那个Server节点
         return nodes.get(0);
     }
     /**
     * 获取物理节点名称
     */
     public static String getPhyName(VirtualServerNode virtualServerNode) {
         return StringUtils.substringBeforeLast(virtualServerNode.getServerNode(), VIRTUAL_SPLIT);
     }
     public synchronized List<VirtualServerNode> getNodes() {
        return nodes;
     }
     private MurmurHash3.HashValue getHash(String key) {
        return MurmurHash3.murmurhash3_x64_128(key, seed);
     }
}
