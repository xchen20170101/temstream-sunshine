#pragma once

#include <QObject>
#include <QHash>
#include <QString>

class ErrorMessages : public QObject
{
    Q_OBJECT

public:
    explicit ErrorMessages(QObject* parent = nullptr);

    Q_INVOKABLE QString translate(const QString& code) const;

private:
    QHash<QString, QString> m_messages;
};
