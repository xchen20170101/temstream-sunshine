package utils

import (
	"crypto/aes"
	"crypto/cipher"
	"crypto/md5"

	//"crypto/rand"
	"encoding/base64"
	"encoding/hex"
	//"io"
)

var key []byte = []byte("01234567890123456789012345678923")

func calculateMD5(input string) string {
	// 创建一个MD5哈希对象
	hasher := md5.New()

	// 将字符串转换为字节数组并写入哈希对象
	hasher.Write([]byte(input))

	// 计算哈希值
	hashBytes := hasher.Sum(nil)

	// 将哈希值转换为十六进制字符串
	hashString := hex.EncodeToString(hashBytes)

	return hashString
}

func decrypt(ciphertext string) (string, error) {
	// 解码Base64
	ciphertextBytes, err := base64.StdEncoding.DecodeString(ciphertext)
	if err != nil {
		return "", err
	}

	// 创建 AES 密码块
	block, err := aes.NewCipher(key)
	if err != nil {
		return "", err
	}

	// 提取初始向量
	iv := ciphertextBytes[:aes.BlockSize]
	ciphertextBytes = ciphertextBytes[aes.BlockSize:]

	// 使用 AES 解密模式创建一个解密流
	mode := cipher.NewCBCDecrypter(block, iv)
	mode.CryptBlocks(ciphertextBytes, ciphertextBytes)

	// 去除填充
	padding := int(ciphertextBytes[len(ciphertextBytes)-1])
	decryptedStr := ciphertextBytes[:len(ciphertextBytes)-padding]

	// 返回解密后的结果
	return string(decryptedStr), nil
}
