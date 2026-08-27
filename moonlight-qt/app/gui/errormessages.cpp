#include "errormessages.h"

ErrorMessages::ErrorMessages(QObject* parent) :
    QObject(parent)
{
    m_messages.insert(QStringLiteral("User.PasswordIsWrong"), QStringLiteral(u"\u7528\u6237\u540D\u6216\u5BC6\u7801\u9519\u8BEF\uFF0C\u8BF7\u91CD\u65B0\u8F93\u5165"));
    m_messages.insert(QStringLiteral("User.Disable"), QStringLiteral(u"\u8D26\u53F7\u5DF2\u88AB\u7981\u7528\uFF0C\u8BF7\u8054\u7CFB\u7BA1\u7406\u5458"));
    m_messages.insert(QStringLiteral("User.NoPermission"), QStringLiteral(u"\u65E0\u8BBF\u95EE\u6743\u9650\uFF0C\u8BF7\u4F7F\u7528\u6709\u6743\u9650\u7684\u8D26\u53F7\u767B\u5F55"));
    m_messages.insert(QStringLiteral("User.NotExist"), QStringLiteral(u"\u7528\u6237\u4E0D\u5B58\u5728"));
    m_messages.insert(QStringLiteral("User.Exists"), QStringLiteral(u"\u7528\u6237\u5DF2\u5B58\u5728"));
    m_messages.insert(QStringLiteral("User.BuiltIn"), QStringLiteral(u"\u5185\u7F6E\u8D26\u53F7\u4E0D\u5141\u8BB8\u6B64\u64CD\u4F5C"));
    m_messages.insert(QStringLiteral("User.CreateFailed"), QStringLiteral(u"\u521B\u5EFA\u7528\u6237\u5931\u8D25\uFF0C\u8BF7\u7A0D\u540E\u91CD\u8BD5"));
    m_messages.insert(QStringLiteral("User.HasBind"), QStringLiteral(u"\u8BE5\u7528\u6237\u5B58\u5728\u7ED1\u5B9A\u5173\u7CFB\uFF0C\u8BF7\u5148\u89E3\u7ED1"));
    m_messages.insert(QStringLiteral("User.LogoutFailed"), QStringLiteral(u"\u9000\u51FA\u767B\u5F55\u5931\u8D25\uFF0C\u8BF7\u7A0D\u540E\u91CD\u8BD5"));
    m_messages.insert(QStringLiteral("User.LicenseExpired"), QStringLiteral(u"\u6388\u6743\u5DF2\u8FC7\u671F\uFF0C\u8BF7\u7EED\u671F\u540E\u91CD\u8BD5"));
    m_messages.insert(QStringLiteral("User.LicenseExists"), QStringLiteral(u"\u6388\u6743\u7801\u5DF2\u5B58\u5728"));
    m_messages.insert(QStringLiteral("User.LicenseActiveFailed"), QStringLiteral(u"\u6388\u6743\u7801\u6FC0\u6D3B\u5931\u8D25\uFF0C\u8BF7\u68C0\u67E5\u6388\u6743\u7801\u662F\u5426\u6B63\u786E"));

    m_messages.insert(QStringLiteral("Device.NotExist"), QStringLiteral(u"\u8BBE\u5907\u4E0D\u5B58\u5728\u6216\u5DF2\u79BB\u7EBF"));
    m_messages.insert(QStringLiteral("Device.NotFound"), QStringLiteral(u"\u672A\u627E\u5230\u6307\u5B9A\u8BBE\u5907"));
    m_messages.insert(QStringLiteral("Device.AlreadyBind"), QStringLiteral(u"\u8BBE\u5907\u5DF2\u88AB\u7ED1\u5B9A\uFF0C\u8BF7\u5148\u89E3\u7ED1"));
    m_messages.insert(QStringLiteral("Device.NotBind"), QStringLiteral(u"\u8BBE\u5907\u672A\u7ED1\u5B9A"));
    m_messages.insert(QStringLiteral("Device.PasswordRequired"), QStringLiteral(u"\u8BBE\u5907\u9700\u8981\u5BC6\u7801"));
    m_messages.insert(QStringLiteral("Device.PasswordMismatch"), QStringLiteral(u"\u8BBE\u5907\u5BC6\u7801\u4E0D\u6B63\u786E"));
    m_messages.insert(QStringLiteral("Device.DeleteVMFailed"), QStringLiteral(u"\u5220\u9664\u8BBE\u5907\u5931\u8D25\uFF0C\u8BF7\u7A0D\u540E\u91CD\u8BD5"));
    m_messages.insert(QStringLiteral("Device.DeleteBindFailed"), QStringLiteral(u"\u5220\u9664\u8BBE\u5907\u7ED1\u5B9A\u5931\u8D25\uFF0C\u8BF7\u7A0D\u540E\u91CD\u8BD5"));

    m_messages.insert(QStringLiteral("Bind.CreateFailed"), QStringLiteral(u"\u521B\u5EFA\u7ED1\u5B9A\u5173\u7CFB\u5931\u8D25\uFF0C\u8BF7\u7A0D\u540E\u91CD\u8BD5"));
    m_messages.insert(QStringLiteral("Bind.UpdateFailed"), QStringLiteral(u"\u66F4\u65B0\u7ED1\u5B9A\u5173\u7CFB\u5931\u8D25\uFF0C\u8BF7\u7A0D\u540E\u91CD\u8BD5"));

    m_messages.insert(QStringLiteral("Common.InvalidParam"), QStringLiteral(u"\u8BF7\u6C42\u53C2\u6570\u65E0\u6548"));
    m_messages.insert(QStringLiteral("Common.InternalError"), QStringLiteral(u"\u670D\u52A1\u5668\u5185\u90E8\u9519\u8BEF\uFF0C\u8BF7\u7A0D\u540E\u91CD\u8BD5"));
}

QString ErrorMessages::translate(const QString& code) const
{
    if (code.isEmpty()) {
        return QString();
    }

    auto it = m_messages.find(code);
    if (it != m_messages.end()) {
        return it.value();
    }

    if (code.startsWith(QStringLiteral("User.")) ||
        code.startsWith(QStringLiteral("Device.")) ||
        code.startsWith(QStringLiteral("Bind.")) ||
        code.startsWith(QStringLiteral("Common."))) {
        return QStringLiteral(u"\u64CD\u4F5C\u5931\u8D25\uFF0C\u8BF7\u7A0D\u540E\u91CD\u8BD5");
    }

    return code;
}