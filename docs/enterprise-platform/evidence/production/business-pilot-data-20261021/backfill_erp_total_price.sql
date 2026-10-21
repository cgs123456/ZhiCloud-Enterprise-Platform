USE zhicloud_sandbox_v140;
UPDATE erp_sale_out SET total_price = total_amount WHERE total_price IS NULL AND total_amount IS NOT NULL;
UPDATE erp_sale_return SET total_price = total_amount WHERE total_price IS NULL AND total_amount IS NOT NULL;
UPDATE erp_purchase_in SET total_price = total_amount WHERE total_price IS NULL AND total_amount IS NOT NULL;
UPDATE erp_purchase_return SET total_price = total_amount WHERE total_price IS NULL AND total_amount IS NOT NULL;
UPDATE erp_sale_order SET total_price = total_amount WHERE total_price IS NULL AND total_amount IS NOT NULL;
UPDATE erp_purchase_order SET total_price = total_amount WHERE total_price IS NULL AND total_amount IS NOT NULL;
