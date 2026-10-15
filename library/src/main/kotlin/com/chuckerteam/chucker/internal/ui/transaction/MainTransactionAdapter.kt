package com.chuckerteam.chucker.internal.ui.transaction

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.ImageViewCompat
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.chuckerteam.chucker.R
import com.chuckerteam.chucker.databinding.ChuckerListItemTransactionBinding
import com.chuckerteam.chucker.internal.data.entity.HttpTransaction
import com.chuckerteam.chucker.internal.data.entity.HttpTransactionTuple
import com.chuckerteam.chucker.internal.support.TransactionUiDiffCallback
import com.chuckerteam.chucker.internal.ui.TransactionUi
import javax.net.ssl.HttpsURLConnection

/**
 * Adapter for displaying a list of [HttpTransactionTuple] items in a RecyclerView.
 * Supports single-tap and long-tap interaction for click and multi-select operations.
 *
 * @param context The application context for resource access.
 * @param onTransactionClick Callback invoked on single click with transaction ID.
 * @param onTransactionLongClick Callback invoked on long click with transaction ID.
 */
internal class MainTransactionAdapter internal constructor(
    context: Context,
    private val onTransactionClick: (TransactionUi) -> Unit,
    private val onTransactionLongClick: (TransactionUi) -> Unit,
) : ListAdapter<TransactionUi, MainTransactionAdapter.ViewHolder>(
    TransactionUiDiffCallback,
) {
    private val colorDefault: Int = ContextCompat.getColor(context, R.color.chucker_status_default)
    private val colorRequested: Int =
        ContextCompat.getColor(context, R.color.chucker_status_requested)
    private val colorError: Int = ContextCompat.getColor(context, R.color.chucker_status_error)
    private val color500: Int = ContextCompat.getColor(context, R.color.chucker_status_500)
    private val color400: Int = ContextCompat.getColor(context, R.color.chucker_status_400)
    private val color300: Int = ContextCompat.getColor(context, R.color.chucker_status_300)
    private val colorSelected =
        ContextCompat.getColor(context, R.color.chucker_status_multiple_selection)

    /** Fallback background from theme for unselected items */
    private val backgroundSelectableAttr =
        TypedValue().also {
            context.theme.resolveAttribute(android.R.attr.selectableItemBackground, it, true)
        }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ViewHolder {
        val viewBinding =
            ChuckerListItemTransactionBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false,
            )
        return ViewHolder(viewBinding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(
            transaction = getItem(position),
            onTransactionClick = onTransactionClick,
            onTransactionLongClick = onTransactionLongClick
        )
    }

    inner class ViewHolder(
        private val itemBinding: ChuckerListItemTransactionBinding,
    ) : RecyclerView.ViewHolder(itemBinding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(
            transaction: TransactionUi,
            onTransactionClick: (TransactionUi) -> Unit,
            onTransactionLongClick: (TransactionUi) -> Unit
        ) {
            itemView.setOnClickListener { onTransactionClick(transaction) }
            itemView.setOnLongClickListener {
                onTransactionLongClick(transaction)
                true
            }

            itemBinding.apply {
                if (transaction.isSelected) {
                    itemView.setBackgroundColor(colorSelected)
                } else {
                    itemView.setBackgroundResource(backgroundSelectableAttr.resourceId)
                }
                displayGraphQlFields(transaction.graphQlOperationName, transaction.isGraphQl)
                path.text = transaction.path
                host.text = transaction.host
                timeStart.text = transaction.requestDate

                setProtocolImage(if (transaction.isSsl) ProtocolResources.Https() else ProtocolResources.Http())

                if (transaction.status == HttpTransaction.Status.Complete) {
                    code.text = transaction.responseCode.toString()
                    duration.text = transaction.durationString
                    size.text = transaction.totalSizeString
                } else {
                    code.text = ""
                    duration.text = ""
                    size.text = ""
                }

                if (transaction.status == HttpTransaction.Status.Failed) {
                    code.text = "!!!"
                }
            }

            setStatusColor(transaction)
        }

        private fun setProtocolImage(resources: ProtocolResources) {
            itemBinding.ssl.setImageDrawable(
                AppCompatResources.getDrawable(itemView.context, resources.icon),
            )
            ImageViewCompat.setImageTintList(
                itemBinding.ssl,
                ColorStateList.valueOf(ContextCompat.getColor(itemView.context, resources.color)),
            )
        }

        private fun setStatusColor(transaction: TransactionUi) {
            val color = when {
                (transaction.status === HttpTransaction.Status.Failed) -> colorError
                (transaction.status === HttpTransaction.Status.Requested) -> colorRequested
                (transaction.responseCode == null) -> colorDefault
                (transaction.responseCode >= HttpsURLConnection.HTTP_INTERNAL_ERROR) -> color500
                (transaction.responseCode >= HttpsURLConnection.HTTP_BAD_REQUEST) -> color400
                (transaction.responseCode >= HttpsURLConnection.HTTP_MULT_CHOICE) -> color300
                else -> colorDefault
            }
            itemBinding.code.setTextColor(color)
            itemBinding.path.setTextColor(color)
        }
    }
}

private fun ChuckerListItemTransactionBinding.displayGraphQlFields(
    graphQlOperationName: String?,
    graphQLDetected: Boolean,
) {
    graphqlIcon.isVisible = graphQLDetected
    graphqlPath.isVisible = graphQLDetected

    if (graphQLDetected) {
        graphqlPath.text = graphQlOperationName
            ?: root.resources.getString(R.string.chucker_graphql_operation_is_empty)
    }
}
