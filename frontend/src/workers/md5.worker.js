import SparkMD5 from "spark-md5";

const cancelledTasks = new Set();

self.onmessage = async (event) => {
  const payload = event.data || {};

  if (payload.type === "cancel") {
    if (payload.uid) {
      cancelledTasks.add(payload.uid);
    }
    return;
  }

  if (payload.type !== "compute") {
    return;
  }

  const { uid, file, chunkSize } = payload;
  if (!uid || !file) {
    self.postMessage({ type: "error", uid, errorMsg: "MD5 参数无效" });
    return;
  }

  const realChunkSize = Number(chunkSize) > 0 ? Number(chunkSize) : 5 * 1024 * 1024;
  const totalChunks = Math.max(1, Math.ceil(file.size / realChunkSize));
  const spark = new SparkMD5.ArrayBuffer();

  try {
    for (let chunkIndex = 0; chunkIndex < totalChunks; chunkIndex++) {
      if (cancelledTasks.has(uid)) {
        cancelledTasks.delete(uid);
        self.postMessage({ type: "cancelled", uid });
        return;
      }

      const start = chunkIndex * realChunkSize;
      const end = Math.min(start + realChunkSize, file.size);
      const chunk = file.slice(start, end);
      const buffer = await chunk.arrayBuffer();
      spark.append(buffer);

      const progress = Math.floor(((chunkIndex + 1) / totalChunks) * 100);
      self.postMessage({ type: "progress", uid, progress });
    }

    const md5 = spark.end();
    self.postMessage({ type: "done", uid, md5 });
  } catch (error) {
    self.postMessage({
      type: "error",
      uid,
      errorMsg: error instanceof Error ? error.message : "MD5 计算失败",
    });
  }
};
